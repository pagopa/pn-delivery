package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.authorization.CxType;
import it.pagopa.pn.delivery.svc.search.TimelineEnricher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR;

@Component
@Slf4j
@AllArgsConstructor
public class NotificationRetrieverService {
    private final NotificationDao notificationDao;
    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private final PnMandateClientImpl pnMandateClient;

    public InternalNotification getInternalNotification(String iun) {
        Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun, true);
        if (optNotification.isPresent()) {
            return optNotification.get();
        } else {
            String msg = String.format("Error retrieving Internal Notification with iun=%s", iun);
            log.debug(msg);
            throw new PnNotificationNotFoundException(msg);
        }
    }

    public <T extends NotificationDetail> T loadAndEnrichNotificationDetail(
            String iun,
            boolean withTimeline,
            boolean requestBySender,
            String senderId,
            T detail,
            TimelineEnricher<T> enricher) {

        InternalNotification notification = getInternalNotification(iun);
        detail.setNotification(notification);
        if (withTimeline) {
            enricher.enrichNotificationDetail(detail, requestBySender);
        }
        labelizeGroup(notification, senderId);
        return detail;
    }

    public <T extends NotificationDetail> T loadCheckAndEnrichNotificationDetail(
            String iun,
            String senderId,
            List<String> groups,
            T detail,
            TimelineEnricher<T> enricher) {
        InternalNotification notification = getInternalNotification(iun);
        detail.setNotification(notification);
        checkSenderId(iun, notification.getSenderPaId(), senderId, notification.getGroup(), groups);
        enricher.enrichNotificationDetail(detail, true);
        labelizeGroup(notification, senderId);
        return detail;
    }

    public String resolveIunFromRequestId(String senderId, String paProtocolNumber, String idempotenceToken) {
        Optional<String> optionalRequestId = notificationDao.getRequestId(senderId, paProtocolNumber, idempotenceToken);
        if (optionalRequestId.isEmpty()) {
            String msg = String.format("Unable to find requestId for senderId=%s paProtocolNumber=%s idempotenceToken=%s", senderId, paProtocolNumber, idempotenceToken);
            throw new PnNotificationNotFoundException(msg);
        }
        return new String(Base64Utils.decodeFromString(optionalRequestId.get()));
    }

    private void labelizeGroup(InternalNotification notification, String senderId) {
        String notificationGroup = notification.getGroup();
        // no notification or no sender id
        if (notificationGroup == null || notificationGroup.isEmpty() || senderId == null) {
            return;
        }
        List<PaGroup> groups = pnExternalRegistriesClient.getGroups(senderId, false);
        if (!groups.isEmpty()) {
            groups.stream()
                    .filter(g -> Objects.requireNonNull(g.getId()).equals(notificationGroup))
                    .findAny().ifPresent(group -> notification.setGroup(group.getName()));
        }
    }

    private void checkSenderId(String iun, String notificationSenderPaId, String senderId, String notificationGroup, List<String> groups) {
        if (!notificationSenderPaId.equals(senderId))
            throw new PnNotificationNotFoundException(
                    String.format("Unable to find notification with iun=%s for senderId=%s", iun, senderId)
            );
        if (StringUtils.hasText(notificationGroup) && !CollectionUtils.isEmpty(groups)
                && !groups.contains(notificationGroup)) {
            throw new PnNotificationNotFoundException(
                    String.format("Unable to find notification with iun=%s for senderId=%s in groups=%s", iun, senderId, groups)
            );
        }
    }

    public static void filterRecipients(InternalNotification internalNotification, InternalAuthHeader internalAuthHeader, int recipientIndex) {
        if (!CxType.PA.name().equals(internalAuthHeader.cxType())) {
            //se il servizio è invocato da un destinatario (o suo delegato), devo vedere tutti i dati in chiaro solo per lo specifico destinatario
            //filtro (cyType != PA) superfluo poiché attualmente il servizio è invocato solo lato destinatario

            //"pulisco gli altri destinatari"
            var filteredNotificationRecipients = new ArrayList<NotificationRecipient>();
            for (int i = 0; i < internalNotification.getRecipients().size(); i++) {
                NotificationRecipient recipient = internalNotification.getRecipients().get(i);
                if (i != recipientIndex) {
                    recipient = NotificationRecipient.builder()
                            .recipientType(recipient.getRecipientType())
                            .internalId(recipient.getInternalId())
                            .build();
                }
                filteredNotificationRecipients.add(recipient);
            }

            internalNotification.setRecipients(filteredNotificationRecipients);
        }
    }

    public void checkIUNAndInternalId(String iun, String recipientInternalId, String mandateId, String cxType, List<String> cxGroups) {
        InternalNotification internalNotification = getInternalNotification(iun);

        if (StringUtils.hasText(mandateId)) {
            // Se è presente il mandateId, il campo recipientInternalId è valorizzato con l'id del delegato

            checkMandateForNotificationDetail(recipientInternalId,
                    mandateId,
                    internalNotification.getSenderPaId(),
                    iun,
                    cxType,
                    cxGroups,
                    internalNotification.getSentAt());
        } else {
            boolean isRecipientOfNotification = internalNotification.getRecipientIds().stream()
                    .anyMatch(recId -> recId.equalsIgnoreCase(recipientInternalId));

            if (!isRecipientOfNotification) {
                throw new PnForbiddenException(ERROR_CODE_DELIVERY_USER_ID_NOT_RECIPIENT_OR_DELEGATOR);
            }
        }
    }

    public @NotNull InternalMandateDto checkMandateForNotificationDetail(String userId,
                                                                         String mandateId,
                                                                         String paId,
                                                                         String iun,
                                                                         String recipientType,
                                                                         List<String> cxGroups,
                                                                         OffsetDateTime notificationSentAt) {
        CxTypeAuthFleet cxTypeAuthFleet = StringUtils.hasText(recipientType) ? CxTypeAuthFleet.valueOf(recipientType) : null;
        String rootSenderId = pnExternalRegistriesClient.getRootSenderId(paId);
        List<InternalMandateDto> mandates = pnMandateClient.listMandatesByDelegateV2(userId, mandateId, cxTypeAuthFleet, cxGroups, notificationSentAt, iun, rootSenderId);

        if (!mandates.isEmpty()) {
            // Considerando che abbiamo fornito il filtro sul mandateId, dovrebbe esserci al massimo una delega valida.
            InternalMandateDto mandate = mandates.get(0);
            log.info("Valid mandate for notification detail for delegate={}", userId);
            return mandate;
        }

        String message = String.format("Unable to find any mandate for notification detail for delegate=%s with mandateId=%s iun=%s", userId, mandateId, iun);
        log.error(message);
        throw new PnMandateNotFoundException(message);
    }

}

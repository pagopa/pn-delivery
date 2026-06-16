package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.LegalNotificationDetail;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalTimelineEnricher implements TimelineEnricher<LegalNotificationDetail> {

    private final Clock clock;
    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final ModelMapper modelMapper;
    private final RefinementLocalDate refinementLocalDateUtils;
    private final PnDeliveryConfigs cfg;

    @Override
    public void enrichNotificationDetail(LegalNotificationDetail notificationDetail, boolean requestBySender) {
        enrichWithTimelineAndStatusHistory(notificationDetail.getNotification().getIun(), notificationDetail);
        OffsetDateTime refinementDate = findRefinementDate(notificationDetail.getTimeline(), notificationDetail.getNotification().getIun());
        checkDocumentsAvailability(notificationDetail.getNotification(), refinementDate, requestBySender);
    }

    private void enrichWithTimelineAndStatusHistory(String iun, LegalNotificationDetail notification) {
        log.debug("Retrieve timeline for iun={}", iun);
        int numberOfRecipients = notification.getNotification().getRecipients().size();
        OffsetDateTime createdAt = notification.getNotification().getSentAt();

        NotificationHistoryResponse timelineStatusHistoryDto = pnDeliveryPushClient.getTimelineAndStatusHistory(iun, numberOfRecipients, createdAt);

        // la lista arriva già ordinata correttamente
        var timelineList = timelineStatusHistoryDto.getTimeline();

        log.debug("Retrieve status history for notification created at={}", createdAt);

        List<NotificationStatusHistoryElementV26> statusHistory = timelineStatusHistoryDto.getNotificationStatusHistory();

        enrichLegalNotification(notification,
                Objects.requireNonNull(timelineList),
                Objects.requireNonNull(statusHistory),
                Objects.requireNonNull(timelineStatusHistoryDto));
    }

    private void enrichLegalNotification(LegalNotificationDetail notification, List<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV28> timelineList, List<NotificationStatusHistoryElementV26> statusHistory, NotificationHistoryResponse timelineStatusHistoryDto) {
        notification.setTimeline(timelineList.stream()
                        .map(timelineElement -> modelMapper.map(timelineElement, TimelineElementV28.class))
                        .toList());

        notification.setNotificationStatusHistory(statusHistory.stream()
                .map(el -> modelMapper.map(el, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26.class))
                .toList());

        notification.setNotificationStatus(NotificationStatusV26.fromValue(Objects.requireNonNull(timelineStatusHistoryDto.getNotificationStatus()).getValue()));
    }

    protected OffsetDateTime findRefinementDate(List<TimelineElementV28> timeline, String iun) {
        log.debug("Find refinement date iun={}", iun);
        OffsetDateTime refinementDate = null;
        // cerco elemento timeline con category refinement o notificationView
        Optional<TimelineElementV28> optionalMin = timeline
                .stream()
                .filter(tle -> TimelineElementCategoryV28.REFINEMENT.equals(tle.getCategory())
                        || TimelineElementCategoryV28.NOTIFICATION_VIEWED.equals(tle.getCategory()))
                .min(Comparator.comparing(TimelineElementV28::getTimestamp));
        // se trovo la data di perfezionamento della notifica
        if (optionalMin.isPresent()) {
            refinementDate = refinementLocalDateUtils.setLocalRefinementDate(optionalMin.get());
        } else {
            log.debug("Notification iun={} not perfected", iun);
        }
        return refinementDate;
    }

    private void checkDocumentsAvailability(InternalNotification notification, OffsetDateTime refinementDate, boolean requestBySender) {
        log.debug("Check if documents are available for iun={}", notification.getIun());
        notification.setDocumentsAvailable(true);
        if (requestBySender || !isNotificationCancelled(notification)) {
            checkDocumentsRemove(notification, refinementDate);
        } else {
            log.debug("Documents not more available for iun={} because is cancelled", notification.getIun());
            notification.setDocumentsAvailable(false);
            // i documenti vanno rimossi solo se trascorso il tempo
            checkDocumentsRemove(notification, refinementDate);
        }
    }

    private void checkDocumentsRemove(InternalNotification notification, OffsetDateTime refinementDate) {
        log.debug("Check if documents should be removed for iun={}", notification.getIun());
        if (refinementDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(refinementDate.toInstant().truncatedTo(ChronoUnit.DAYS),
                    clock.instant().truncatedTo(ChronoUnit.DAYS));
            if (daysBetween > Long.parseLong(cfg.getMaxDocumentsAvailableDays())) {
                log.debug("Documents not more available for iun={} from={}", notification.getIun(), refinementDate);
                removeDocuments(notification);
            }
        }
    }

    private void removeDocuments(InternalNotification notification) {
        notification.setDocumentsAvailable(false);
        notification.setDocuments(Collections.emptyList());
        for (NotificationRecipient recipient : notification.getRecipients()) {
            List<NotificationPaymentInfo> payments = recipient.getPayments();
            if (!CollectionUtils.isEmpty(payments)) {
                payments.forEach(this::removePaymentAttachment);
            }
        }
    }

    private void removePaymentAttachment(NotificationPaymentInfo notificationPaymentInfo) {
        PagoPaPayment pagoPaPayment = notificationPaymentInfo.getPagoPa();
        if (Objects.nonNull(pagoPaPayment)) {
            // rimuovo allegato di pagamento pagoPA
            pagoPaPayment.setAttachment(null);
        }
        F24Payment f24Payment = notificationPaymentInfo.getF24();
        if (Objects.nonNull(f24Payment)) {
            // rimuovo oggetto di pagamento F24
            notificationPaymentInfo.setF24(null);
        }
    }

    public boolean isNotificationCancelled(InternalNotification notification) {
        var cancellationRequestCategory = TimelineElementCategoryV28.NOTIFICATION_CANCELLATION_REQUEST;
        Optional<TimelineElementV28> cancellationRequestTimeline = notification.getTimeline().stream()
                .filter(timelineElement -> cancellationRequestCategory.equals(timelineElement.getCategory()))
                .findFirst();
        boolean cancellationTimelineIsPresent = cancellationRequestTimeline.isPresent();
        if (cancellationTimelineIsPresent) {
            log.warn("Notification with iun: {} has a request for cancellation", notification.getIun());
        }
        return cancellationTimelineIsPresent;
    }
}

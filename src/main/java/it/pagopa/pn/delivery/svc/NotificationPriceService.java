package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationCostResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponseV23;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import it.pagopa.pn.delivery.models.InternalAsseverationEvent;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONCOSTNOTFOUND;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONMETADATANOTFOUND;

@Service
@Slf4j
public class NotificationPriceService {
    private final Clock clock;
    private final NotificationCostEntityDao notificationCostEntityDao;
    private final NotificationDao notificationDao;
    private final NotificationMetadataEntityDao notificationMetadataEntityDao;
    private final PnDeliveryPushClientImpl deliveryPushClient;

    private final AsseverationEventsProducer asseverationEventsProducer;
    private final RefinementLocalDate refinementLocalDateUtils;


    public static final String ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED = "PN_DELIVERYPUSH_NOTIFICATION_CANCELLED";

    public NotificationPriceService(Clock clock, NotificationCostEntityDao notificationCostEntityDao, NotificationDao notificationDao, NotificationMetadataEntityDao notificationMetadataEntityDao, PnDeliveryPushClientImpl deliveryPushClient, AsseverationEventsProducer asseverationEventsProducer, RefinementLocalDate refinementLocalDateUtils) {
        this.clock = clock;
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.notificationDao = notificationDao;
        this.notificationMetadataEntityDao = notificationMetadataEntityDao;
        this.deliveryPushClient = deliveryPushClient;
        this.asseverationEventsProducer = asseverationEventsProducer;
        this.refinementLocalDateUtils = refinementLocalDateUtils;
    }

    public NotificationPriceResponseV23 getNotificationPrice(String paTaxId, String noticeCode) {
        log.info( "Get notification price for paTaxId={} noticeCode={}", paTaxId, noticeCode );
        InternalNotificationCost internalNotificationCost = getInternalNotificationCost(paTaxId, noticeCode);
        String iun = internalNotificationCost.getIun();
        log.info( "Get notification with iun={}", iun);
        InternalNotification internalNotification = getInternalNotification(iun);
        NotificationFeePolicy notificationFeePolicy = NotificationFeePolicy.fromValue(
                internalNotification.getNotificationFeePolicy().getValue()
        );
        int recipientIdx = internalNotificationCost.getRecipientIdx();
        // se la lista degli id non presente nell'internal notifications la posso recuperare dalla notificationMetadataEntity
        String recipientId = internalNotification.getRecipientIds().get(recipientIdx);
        log.info( "Get notification process cost with iun={} recipientId={} recipientIdx={} feePolicy={}", iun, recipientId, recipientIdx, notificationFeePolicy);

        boolean applyCost = getApplyCost(internalNotification, noticeCode);

        NotificationProcessCostResponse notificationProcessCost = getNotificationProcessCost(iun, recipientId, recipientIdx, notificationFeePolicy, internalNotification.getSentAt(), applyCost, internalNotification.getPaFee(), internalNotification.getVat());

        // invio l'evento di asseverazione sulla coda
        log.info( "Send asseveration event iun={} creditorTaxId={} noticeCode={}", iun, paTaxId, noticeCode );
        asseverationEventsProducer.sendAsseverationEvent(
                createInternalAsseverationEvent(internalNotificationCost, internalNotification)
        );

        // creazione dto response
        return NotificationPriceResponseV23.builder()
                .partialPrice(notificationProcessCost.getPartialCost())
                .totalPrice(notificationProcessCost.getTotalCost())
                .analogCost(notificationProcessCost.getAnalogCost())
                .paFee(notificationProcessCost.getPaFee())
                .sendFee(notificationProcessCost.getSendFee())
                .vat(notificationProcessCost.getVat())
                .refinementDate( refinementLocalDateUtils.setLocalRefinementDate( notificationProcessCost.getRefinementDate() ) )
                .notificationViewDate( refinementLocalDateUtils.setLocalRefinementDate( notificationProcessCost.getNotificationViewDate() ) )
                .iun( iun )
                .build();
    }

    private boolean getApplyCost(InternalNotification internalNotification, String noticeCode){
        for(NotificationRecipient recipient : internalNotification.getRecipients()){
            Optional<PagoPaPayment> optPagoPaPayment = findPagoPaPaymentByNoticeCode(recipient, noticeCode);
            if(optPagoPaPayment.isPresent()) {
                return optPagoPaPayment.get().isApplyCost();
            }
        }

        log.error( "Unable to find recipients or payments for iun={}", internalNotification.getIun());
        throw new PnNotificationNotFoundException( String.format("Unable to find recipients or payments for iun=%s", internalNotification.getIun()));
    }

    private Optional<PagoPaPayment> findPagoPaPaymentByNoticeCode(NotificationRecipient recipient, String noticeCode) {
        if (recipient == null) { return Optional.empty(); }

        return recipient.getPayments().stream()
                .filter(payment -> payment.getPagoPa() != null && payment.getPagoPa().getNoticeCode().equals(noticeCode))
                .map(NotificationPaymentInfo::getPagoPa)
                .findFirst();
    }

    private InternalAsseverationEvent createInternalAsseverationEvent(InternalNotificationCost internalNotificationCost, InternalNotification internalNotification) {
        Instant now = clock.instant();
        String formattedNow = refinementLocalDateUtils.formatInstantToString(now);
        String formattedSentAt = refinementLocalDateUtils.formatInstantToString(internalNotification.getSentAt().toInstant());
        return InternalAsseverationEvent.builder()
                .iun(internalNotificationCost.getIun())
                .notificationSentAt(formattedSentAt)
                .creditorTaxId(internalNotificationCost.getCreditorTaxIdNoticeCode().split("##")[0])
                .noticeCode(internalNotificationCost.getCreditorTaxIdNoticeCode().split("##")[1])
                .senderPaId(internalNotification.getSenderPaId())
                .recipientIdx(internalNotificationCost.getRecipientIdx())
                .debtorPosUpdateDate(formattedNow)
                .recordCreationDate(formattedNow)
                .version(1)
                .moreFields( AsseverationEvent.Payload.AsseverationMoreField.builder().build() )
                .build();
    }

    private InternalNotification getInternalNotification(String iun) {
        Optional<InternalNotification> optionalNotification = notificationDao.getNotificationByIun(iun, false);
        if (optionalNotification.isPresent()) {
            return optionalNotification.get();
        } else {
            log.error( "Unable to find notification for iun={}", iun);
            throw new PnNotificationNotFoundException( String.format("Unable to find notification for iun=%s", iun));
        }
    }

    private NotificationProcessCostResponse getNotificationProcessCost(String iun, String recipientId, int recipientIdx, NotificationFeePolicy notificationFeePolicy, OffsetDateTime sentAt, boolean applyCost, Integer paFee, Integer vat) {
        // controllo che notifica sia stata accettata cercandola nella tabella notificationMetadata tramite PK iun##recipientId
        getNotificationMetadataEntity(iun, recipientId, sentAt);

        // contatto delivery-push per farmi calcolare tramite iun, recipientIdx, notificationFeePolicy costo della notifica
        // delivery-push mi risponde con amount, data perfezionamento presa visione, data perfezionamento decorrenza termini
        try {
            return deliveryPushClient.getNotificationProcessCost(iun, recipientIdx, notificationFeePolicy, applyCost, paFee, vat);
        } catch (Exception exc) {
            // nel caso in cui la risposta da parte di delivery push è un 404, devo controllare che la causale
            // sia per colpa della notifica cancellata. Se si, ritorno a mia volta un 404, altrimenti ritorno
            // direttamente l'exception originale
            if (exc instanceof PnHttpResponseException pnHttpResponseException
                    && pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()
                    && (((PnHttpResponseException) exc).getProblem().getErrors().get(0).getCode().equals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED))) {

                throw new PnNotificationCancelledException("Cannot retrive price for cancelled notification", exc);
            }

            throw exc;
        }
    }

    private void getNotificationMetadataEntity(String iun, String recipientId, OffsetDateTime sentAt) {
        Optional<NotificationMetadataEntity> optionalNotificationMetadataEntity = notificationMetadataEntityDao.get(Key.builder()
                .partitionValue(iun + "##" + recipientId)
                .sortValue( sentAt.toString() )
                .build()
        );
        if (optionalNotificationMetadataEntity.isEmpty()) {
            log.info( "Notification iun={}, recipientId={} not found in NotificationsMetadata", iun, recipientId);
            throw new PnNotFoundException("Notification metadata not found", String.format(
                    "Notification iun=%s, recipientId=%s not found in NotificationsMetadata", iun, recipientId),
                    ERROR_CODE_DELIVERY_NOTIFICATIONMETADATANOTFOUND );
        }
    }

    // NOTA da eliminare poiché non utilizzato da pn-delivery-push
    public NotificationCostResponse getNotificationCost(String paTaxId, String noticeCode) {
        String iun;
        int recipientIdx;
        log.info( "Get notification cost info for paTaxId={} noticeCode={}", paTaxId, noticeCode );
        InternalNotificationCost internalNotificationCost = getInternalNotificationCost(paTaxId, noticeCode);
        iun = internalNotificationCost.getIun();
        recipientIdx = internalNotificationCost.getRecipientIdx();

        return NotificationCostResponse.builder()
                .iun( iun )
                .recipientIdx( recipientIdx )
                .build();
    }

    private InternalNotificationCost getInternalNotificationCost( String paTaxId, String noticeCode ) {
        Optional<InternalNotificationCost> optionalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo( paTaxId, noticeCode );
        if (optionalNotificationCost.isPresent()) {
            return optionalNotificationCost.get();
        } else {
            log.info( "No notification cost info by paTaxId={} noticeCode={}", paTaxId, noticeCode );
            throw new PnNotFoundException("Notification cost not found", String.format( "No notification cost info by paTaxId=%s noticeCode=%s", paTaxId, noticeCode ) , ERROR_CODE_DELIVERY_NOTIFICATIONCOSTNOTFOUND );
        }
    }

    public void removeAllNotificationCostsByIun(String iun) {
        InternalNotification notification = notificationDao.getNotificationByIun(iun, false)
                .orElseThrow(() -> new PnNotificationNotFoundException(String.format("Notification with IUN: %s not found", iun)));

        notification.getRecipients().stream()
                .filter(recipient -> recipient.getPayments() != null)
                .forEach(recipient ->
                        recipient.getPayments()
                                .stream()
                                .filter(notificationPaymentInfo ->
                                        notificationPaymentInfo.getPagoPa() != null
                                )
                                .forEach(notificationPaymentInfo ->
                                        notificationCostEntityDao.deleteWithCheckIun(notificationPaymentInfo.getPagoPa().getCreditorTaxId(), notificationPaymentInfo.getPagoPa().getNoticeCode(), iun)
                                ));
    }
}

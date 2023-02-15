package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentEventsService {

    public static final String NOTIFICATION_NOT_FOUND_MSG = "No notification by iun=";
    public static final String HANDLE_PAYMENT_EVENT_PAGOPA_NO_NOTIFICATION_BY_IUN_MSG = "Handle payment event PagoPa - No notification by iun={} ";
    public static final String HANDLE_PAYMENT_EVENT_F24_NO_NOTIFICATION_BY_IUN_MSG = "Handle payment event F24 - No notification by iun={} ";
    private final PaymentEventsProducer paymentEventsProducer;
    private final NotificationCostEntityDao notificationCostEntityDao;
    private final NotificationDao notificationDao;
    private final PnDataVaultClientImpl dataVaultClient;
    private final CheckAuthComponent checkAuthComponent;

    public PaymentEventsService(PaymentEventsProducer paymentEventsProducer, NotificationCostEntityDao notificationCostEntityDao, NotificationDao notificationDao, PnDataVaultClientImpl dataVaultClient, CheckAuthComponent checkAuthComponent) {
        this.paymentEventsProducer = paymentEventsProducer;
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.notificationDao = notificationDao;
        this.dataVaultClient = dataVaultClient;
        this.checkAuthComponent = checkAuthComponent;
    }

    public String handlePaymentEventsPagoPa(String cxType, String xPagopaPnCxId, PaymentEventsRequestPagoPa paymentEventsRequest) {
        List<PaymentEventPagoPa> paymentRequests = paymentEventsRequest.getEvents();
        List<InternalPaymentEvent> paymentEvents = new ArrayList<>( paymentRequests.size() );

        String iun = null;
        String creditorTaxId;
        String noticeCode;
        int recipientIdx;
        String recipientType;
        PnDeliveryPaymentEvent.PaymentType paymentType = PnDeliveryPaymentEvent.PaymentType.PAGOPA;

        // per ogni evento di pagamento nella lista di eventi
        for ( PaymentEventPagoPa paymentRequest : paymentRequests ) {
            creditorTaxId = paymentRequest.getCreditorTaxId();
            noticeCode = paymentRequest.getNoticeCode();

            InternalNotification internalNotification;

            // recupero iun, recipientIdx e recipientType dalla NotificationsCost table
            Optional<InternalNotificationCost> optionalInternalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo(creditorTaxId, noticeCode);
            if (optionalInternalNotificationCost.isPresent()) {
                InternalNotificationCost internalNotificationCost = optionalInternalNotificationCost.get();
                iun = internalNotificationCost.getIun();
                recipientIdx = internalNotificationCost.getRecipientIdx();
                recipientType = internalNotificationCost.getRecipientType();
            } else {
                log.info( "Handle payment event - No notification by creditorTaxId={} noticeCode={}", creditorTaxId, noticeCode);
                throw new PnNotificationNotFoundException( String.format( "No notification by creditorTaxId=%s noticeCode=%s", creditorTaxId, noticeCode) );
            }

            // recupero notifica tramite iun
            Optional<InternalNotification> optionalInternalNotification = notificationDao.getNotificationByIun( iun );
            if ( optionalInternalNotification.isPresent() ) {
                internalNotification = optionalInternalNotification.get();
            } else {
                log.info( HANDLE_PAYMENT_EVENT_PAGOPA_NO_NOTIFICATION_BY_IUN_MSG, iun );
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            // controllo autorizzazione
            ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, xPagopaPnCxId, null, iun, recipientIdx );
            AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, internalNotification );

            if ( !authorizationOutcome.isAuthorized() ) {
                log.info( HANDLE_PAYMENT_EVENT_PAGOPA_NO_NOTIFICATION_BY_IUN_MSG, iun );
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            // creo evento InternalPaymentEvent
            paymentEvents.add( InternalPaymentEvent.builder()
                    .iun( iun )
                    .recipientType( PnDeliveryPaymentEvent.RecipientType.valueOf( recipientType ) )
                    .recipientIdx( recipientIdx )
                    .paymentDate( paymentRequest.getPaymentDate().toInstant() )
                    .paymentType( paymentType )
                    .creditorTaxId( creditorTaxId )
                    .noticeCode( noticeCode )
                    .build()
            );
        }
        // pubblico eventi sulla coda di delivery-push
        paymentEventsProducer.sendPaymentEvents( paymentEvents );
        return iun;
    }

    public void handlePaymentEventsF24(String cxTypePa, String cxIdPaId, PaymentEventsRequestF24 paymentEventsRequestF24) {
        List<InternalPaymentEvent> paymentEvents = new ArrayList<>( paymentEventsRequestF24.getEvents().size() );

        for (PaymentEventF24 paymentEventF24 : paymentEventsRequestF24.getEvents() ) {
            String iun = paymentEventF24.getIun();
            int recipientIdx;
            InternalNotification internalNotification;

            Optional<InternalNotification> optionalInternalNotification = notificationDao.getNotificationByIun( iun );
            if ( optionalInternalNotification.isPresent() ) {
                internalNotification = optionalInternalNotification.get();

                // opacizzo recipientTaxId tramite pn-datavault
                String recipientInternalId = dataVaultClient.ensureRecipientByExternalId(  RecipientType.valueOf( paymentEventF24.getRecipientType() ), paymentEventF24.getRecipientTaxId() );
                List<NotificationRecipient> internalNotificationRecipients = internalNotification.getRecipients();
                Optional<NotificationRecipient> optionalNotificationRecipient = internalNotificationRecipients.stream().filter(rec -> rec.getInternalId().equals( recipientInternalId )).findFirst();
                if (optionalNotificationRecipient.isPresent() ) {
                    // setto recipientIdx del destinatario che ha effettuato il pagamento
                    NotificationRecipient notificationRecipient = optionalNotificationRecipient.get();
                    recipientIdx = internalNotification.getRecipientIds().indexOf( notificationRecipient.getInternalId() );
                } else {
                    log.info( "Handle payment event - No notification iun={} with recipientInternalId={}", iun, recipientInternalId );
                    throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun + " with recipientInternalId="+ recipientInternalId );
                }
            } else {
                log.info( HANDLE_PAYMENT_EVENT_F24_NO_NOTIFICATION_BY_IUN_MSG, iun );
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            // controllo autorizzazione
            ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxTypePa, cxIdPaId, null, iun, recipientIdx );
            AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, internalNotification );

            if ( !authorizationOutcome.isAuthorized() ) {
                log.info( HANDLE_PAYMENT_EVENT_F24_NO_NOTIFICATION_BY_IUN_MSG, iun );
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            paymentEvents.add( InternalPaymentEvent.builder()
                    .iun( iun )
                    .recipientType( PnDeliveryPaymentEvent.RecipientType.valueOf( paymentEventF24.getRecipientType() ) )
                    .recipientIdx( recipientIdx )
                    .paymentDate( paymentEventF24.getPaymentDate().toInstant() )
                    .paymentType( PnDeliveryPaymentEvent.PaymentType.F24 )
                    .build()
            );
        }
        // pubblico eventi sulla coda di delivery-push
        paymentEventsProducer.sendPaymentEvents( paymentEvents );
    }
}

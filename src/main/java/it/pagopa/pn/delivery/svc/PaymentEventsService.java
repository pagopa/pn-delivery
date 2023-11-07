package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentEventsService {

    public static final String NOTIFICATION_NOT_FOUND_MSG = "No notification by iun=";
    public static final String HANDLE_PAYMENT_EVENT_PAGOPA_NO_NOTIFICATION_BY_IUN_MSG = "Handle payment event PagoPa - No notification by iun={} ";
    public static final String HANDLE_PAYMENT_EVENT_F24_NO_NOTIFICATION_BY_IUN_MSG = "Handle payment event F24 - No notification by iun={} ";
    public static final String PAYMENT_SOURCE_CHANNEL_PA = "PA";
    public static final String PAYMENT_SOURCE_CHANNEL_EXTERNAL_REGISTRY = "EXTERNAL_REGISTRY";

    private final PaymentEventsProducer paymentEventsProducer;
    private final NotificationCostEntityDao notificationCostEntityDao;
    private final NotificationDao notificationDao;
    private final CheckAuthComponent checkAuthComponent;

    public PaymentEventsService(PaymentEventsProducer paymentEventsProducer, NotificationCostEntityDao notificationCostEntityDao, NotificationDao notificationDao, CheckAuthComponent checkAuthComponent) {
        this.paymentEventsProducer = paymentEventsProducer;
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.notificationDao = notificationDao;
        this.checkAuthComponent = checkAuthComponent;
    }
    public String handlePaymentEventsPagoPa(String cxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, PaymentEventsRequestPagoPa paymentEventsRequest) {
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

            InternalNotificationCost internalNotificationCost = getInternalNotificationCost(creditorTaxId, noticeCode);

            iun = internalNotificationCost.getIun();
            recipientIdx = internalNotificationCost.getRecipientIdx();
            recipientType = internalNotificationCost.getRecipientType();

            InternalNotification internalNotification;
            // recupero notifica tramite iun
            Optional<InternalNotification> optionalInternalNotification = notificationDao.getNotificationByIun( iun, false );
            if ( optionalInternalNotification.isPresent() ) {
                internalNotification = optionalInternalNotification.get();
            } else {
                log.info( HANDLE_PAYMENT_EVENT_PAGOPA_NO_NOTIFICATION_BY_IUN_MSG, iun );
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            // controllo autorizzazione
            ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, xPagopaPnCxId, null, xPagopaPnCxGroups, iun, recipientIdx );
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
                    .paymentSourceChannel( PAYMENT_SOURCE_CHANNEL_PA )
                    .paymentDate( Instant.parse( paymentRequest.getPaymentDate()) )
                    .uncertainPaymentDate( false )
                    .paymentType( paymentType )
                    .paymentAmount( paymentRequest.getAmount() )
                    .creditorTaxId( creditorTaxId )
                    .noticeCode( noticeCode )
                    .build()
            );
        }
        // pubblico eventi sulla coda di delivery-push
        paymentEventsProducer.sendPaymentEvents( paymentEvents );
        return iun;
    }

    @NotNull
    private InternalNotificationCost getInternalNotificationCost(String creditorTaxId, String noticeCode) {
        // recupero iun, recipientIdx e recipientType dalla NotificationsCost table
        Optional<InternalNotificationCost> optionalInternalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo(creditorTaxId, noticeCode);
        if (optionalInternalNotificationCost.isPresent()) {
            return optionalInternalNotificationCost.get();
        } else {
            log.info( "Handle payment event - No notification by creditorTaxId={} noticeCode={}", creditorTaxId, noticeCode);
            throw new PnNotificationNotFoundException( String.format( "No notification by creditorTaxId=%s noticeCode=%s", creditorTaxId, noticeCode) );
        }
    }
}
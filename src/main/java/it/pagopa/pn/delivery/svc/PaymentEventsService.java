package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEvents;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequest;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentEventsService {

    public static final String NOTIFICATION_NOT_FOUND_MSG = "No notification by iun=";
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

    public void handlePaymentEvents(String cxType, String xPagopaPnCxId, PaymentEventsRequest paymentEventsRequest) {
        List<PaymentEvents> paymentRequests = paymentEventsRequest.getEvents();
        List<InternalPaymentEvent> paymentEvents = new ArrayList<>( paymentRequests.size() );

        // per ogni evento di pagamento nella lista di eventi
        for ( PaymentEvents paymentRequest : paymentRequests ) {
            String creditorTaxId = paymentRequest.getCreditorTaxId();
            String noticeCode = paymentRequest.getNoticeCode();
            String iun = paymentRequest.getIun();
            int recipientIdx;
            String recipientType = paymentRequest.getRecipientType();
            PnDeliveryPaymentEvent.PaymentType paymentType = PnDeliveryPaymentEvent.PaymentType.F24;

            InternalNotification internalNotification;

            // se presenti creditorTaxId e noticeNumber
            if( StringUtils.hasText( creditorTaxId ) && StringUtils.hasText( noticeCode ) ) {
                // recupero iun, recipientIdx e recipientType dalla NotificationsCost table
                paymentType = PnDeliveryPaymentEvent.PaymentType.PAGOPA;
                iun = getIun(creditorTaxId, noticeCode);
            }

            // recupero notifica tramite iun
            Optional<InternalNotification> optionalInternalNotification = notificationDao.getNotificationByIun( paymentRequest.getIun() );
            if ( optionalInternalNotification.isPresent() ) {
                internalNotification = optionalInternalNotification.get();
                // opacizzo recipientTaxId tramite pn-datavault
                String recipientInternalId = dataVaultClient.ensureRecipientByExternalId(  RecipientType.valueOf( paymentRequest.getRecipientType() ), paymentRequest.getRecipientTaxId() );
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
                log.info( "Handle payment event - No notification by iun={} ", iun);
                throw new PnNotificationNotFoundException( NOTIFICATION_NOT_FOUND_MSG + iun );
            }

            // controllo autorizzazione
            ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, xPagopaPnCxId, null, iun, recipientIdx );
            AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, internalNotification );

            if ( !authorizationOutcome.isAuthorized() ) {
                log.info( "Handle payment event - No notification by iun={} ", iun);
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
    }

    private String getIun(String creditorTaxId, String noticeCode) {
        String iun;
        Optional<InternalNotificationCost> optionalInternalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo(creditorTaxId, noticeCode);
        if (optionalInternalNotificationCost.isPresent()) {
            InternalNotificationCost internalNotificationCost = optionalInternalNotificationCost.get();
            iun = internalNotificationCost.getIun();
        } else {
            log.info( "Handle payment event - No notification by creditorTaxId={} noticeCode={}", creditorTaxId, noticeCode);
            throw new PnNotificationNotFoundException( String.format( "No notification by creditorTaxId=%s noticeCode=%s", creditorTaxId, noticeCode) );
        }
        return iun;
    }
}

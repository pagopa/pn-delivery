package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

class PaymentEventsServiceTest {
    public static final String CREDITOR_TAX_ID = "77777777777";
    public static final String SENDER_PA_ID = "sender_pa_id";
    public static final String CX_TYPE_PA = "PA";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    public static final String NOTICE_CODE = "123456789012345678";
    public static final String IUN = "fake_IUN";
    public static final String RECIPIENT_TYPE_PF = "PF";
    public static final String PAYMENT_DATE_STRING = "2023-01-17T12:21:00Z";
    public static final String RECIPIENT_TAX_ID = "RSSMRA77E04H501Q";
    public static final String RECIPIENT_INTERNAL_ID = "recipientInternalId";
    private static final Integer PAYMENT_AMOUNT = 1200;
    private static final String PAYMENT_SOURCE_CHANNEL_PA = "PA";
    @Mock
    private PaymentEventsProducer paymentEventsProducer;
    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;
    @Mock
    private NotificationDao notificationDao;
    @Mock
    private CheckAuthComponent checkAuthComponent;

    private PaymentEventsService service;

    @BeforeEach
    void setup() {
        service = new PaymentEventsService( paymentEventsProducer, notificationCostEntityDao, notificationDao, checkAuthComponent );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePaymentEventsPagoPa() {
        // Given
        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( List.of( PaymentEventPagoPa.builder()
                                .paymentDate( PAYMENT_DATE_STRING )
                                .amount( PAYMENT_AMOUNT )
                                .creditorTaxId( CREDITOR_TAX_ID )
                                .noticeCode( NOTICE_CODE )
                        .build()
                ) )
                .build();

        // When
        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .iun(IUN)
                .recipientIdx(0)
                .recipientType(RECIPIENT_TYPE_PF)
                .creditorTaxIdNoticeCode(CREDITOR_TAX_ID + "##" + NOTICE_CODE)
                .build();

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(), Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost));

        InternalNotification internalNotification = createInternalNotification();

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( internalNotification ) );

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(createRecipient(), 0);

        Mockito.when( checkAuthComponent.canAccess( Mockito.any( ReadAccessAuth.class ), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( authorizationOutcome );


        service.handlePaymentEventsPagoPa( CX_TYPE_PA, SENDER_PA_ID, null, paymentEventsRequestPagoPa );

        // Then
        InternalPaymentEvent internalPaymentEvent = InternalPaymentEvent.builder()
                .paymentDate( Instant.parse( PAYMENT_DATE_STRING ) )
                .paymentType( PnDeliveryPaymentEvent.PaymentType.PAGOPA )
                .paymentAmount( PAYMENT_AMOUNT )
                .paymentSourceChannel(PAYMENT_SOURCE_CHANNEL_PA)
                .iun( IUN )
                .creditorTaxId( CREDITOR_TAX_ID )
                .noticeCode( NOTICE_CODE )
                .recipientIdx( 0 )
                .recipientType( PnDeliveryPaymentEvent.RecipientType.PF )
                .build();

        Mockito.verify( paymentEventsProducer ).sendPaymentEvents( List.of( internalPaymentEvent ) );

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePaymentEventsPagoPaNoNotificationsCost() {
        // Given
        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( List.of( PaymentEventPagoPa.builder()
                        .paymentDate( PAYMENT_DATE_STRING )
                        .creditorTaxId( CREDITOR_TAX_ID )
                        .noticeCode( NOTICE_CODE )
                        .build()
                ) )
                .build();

        // When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(), Mockito.anyString() ) )
                .thenReturn( Optional.empty() );


        // Then
        Executable todo = () -> service.handlePaymentEventsPagoPa( CX_TYPE_PA, SENDER_PA_ID, null, paymentEventsRequestPagoPa );

        Assertions.assertThrows(PnNotFoundException.class, todo);

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePaymentEventsPagoPaNoInternalNotification() {
        // Given
        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( List.of( PaymentEventPagoPa.builder()
                        .paymentDate( PAYMENT_DATE_STRING )
                        .creditorTaxId( CREDITOR_TAX_ID )
                        .noticeCode( NOTICE_CODE )
                        .build()
                ) )
                .build();

        // When
        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .iun(IUN)
                .recipientIdx(0)
                .recipientType(RECIPIENT_TYPE_PF)
                .creditorTaxIdNoticeCode(CREDITOR_TAX_ID + "##" + NOTICE_CODE)
                .build();

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(), Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost));


        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.empty() );

        // Then
        Executable todo = () -> service.handlePaymentEventsPagoPa( CX_TYPE_PA, SENDER_PA_ID, null, paymentEventsRequestPagoPa );

        Assertions.assertThrows(PnNotFoundException.class, todo);

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePaymentEventsPagoPaNoAuth() {
        // Given
        PaymentEventsRequestPagoPa paymentEventsRequestPagoPa = PaymentEventsRequestPagoPa.builder()
                .events( List.of( PaymentEventPagoPa.builder()
                        .paymentDate( PAYMENT_DATE_STRING )
                        .creditorTaxId( CREDITOR_TAX_ID )
                        .noticeCode( NOTICE_CODE )
                        .build()
                ) )
                .build();

        // When
        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .iun(IUN)
                .recipientIdx(0)
                .recipientType(RECIPIENT_TYPE_PF)
                .creditorTaxIdNoticeCode(CREDITOR_TAX_ID + "##" + NOTICE_CODE)
                .build();

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(), Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost));

        InternalNotification internalNotification = createInternalNotification();

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( internalNotification ) );

        Mockito.when( checkAuthComponent.canAccess( Mockito.any( ReadAccessAuth.class ), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( AuthorizationOutcome.fail() );


        // Then
        Executable todo = () -> service.handlePaymentEventsPagoPa( CX_TYPE_PA, SENDER_PA_ID, null, paymentEventsRequestPagoPa );

        Assertions.assertThrows(PnNotFoundException.class, todo);

    }


    private InternalNotification createInternalNotification() {
        NotificationRecipient notificationRecipient = createRecipient();
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setIun(IUN);
        internalNotification.setSubject("Subject 01");
        internalNotification.setSenderPaId(SENDER_PA_ID);
        internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        internalNotification.setRecipients(Collections.singletonList(notificationRecipient));
        internalNotification.setRecipientIds(List.of(RECIPIENT_INTERNAL_ID));
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        return internalNotification;
    }

    private NotificationRecipient createRecipient() {
        return NotificationRecipient.builder()
                .internalId( RECIPIENT_INTERNAL_ID )
                .taxId( RECIPIENT_TAX_ID )
                .denomination("Mario Rossi")
                .recipientType( NotificationRecipientV24.RecipientTypeEnum.valueOf( RECIPIENT_TYPE_PF ) )
                .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                        .address("account@dominio.it")
                        .build())
                .build();
    }
}

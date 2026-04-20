package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24.RecipientTypeEnum.PF;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;

class NotificationPriceServiceTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";
    public static final String EXPECTED_REFINEMENT_DATE = "2022-10-07T23:59:59.999999999+02:00";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    public static final String SENT_AT_DATE = "2023-03-08T14:35:39.214793Z";
    public static final String EVENT_DATE = "2023-03-08T15:45:39.753534Z";

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;

    @Mock
    private AsseverationEventsProducer asseverationEventsProducer;

    @Mock
    private NotificationCostServiceFactory notificationCostServiceFactory;

    @Mock
    private NotificationCostServiceMonitor notificationCostServiceMonitor;

    @Mock
    NotificationCostServiceImpl notificationCostService;

    private final RefinementLocalDate refinementLocalDateUtils = new RefinementLocalDate();

    private NotificationPriceService svc;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed( Instant.parse( EVENT_DATE ), ZoneId.of("UTC"));
        svc = new NotificationPriceService(clock, notificationCostEntityDao, notificationDao, notificationCostServiceFactory, notificationCostServiceMonitor, asseverationEventsProducer, refinementLocalDateUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        internalNotification.setNotificationFeePolicy( NotificationFeePolicy.FLAT_RATE );

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .recipientType( RecipientType.PF.getValue() )
                .recipientIdx(0)
                .build();

        NotificationProcessCostResponseInt notificationProcessCostResponseInt = new NotificationProcessCostResponseInt();
        notificationProcessCostResponseInt.setPartialCost( 2000 );
        notificationProcessCostResponseInt.setRefinementDate( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE ) );

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), anyBoolean() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when(notificationCostServiceFactory.getNotificationCostServiceBySentAt(Mockito.any()))
                .thenReturn( notificationCostService );


        ArgumentCaptor<NotificationCostRequest> requestCaptor = ArgumentCaptor.forClass(NotificationCostRequest.class);
        Mockito.when(notificationCostService.getNotificationCost(requestCaptor.capture()))
                .thenReturn(notificationProcessCostResponseInt);


        NotificationPriceResponseV23 response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( 2000, response.getPartialPrice() );
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE ) , response.getRefinementDate() );
        Assertions.assertNull( response.getNotificationViewDate() );

        String formattedEventDate = refinementLocalDateUtils.formatInstantToString(Instant.parse(EVENT_DATE));
        InternalAsseverationEvent asseverationEvent = InternalAsseverationEvent.builder()
                .iun( "iun" )
                .senderPaId( "senderPaId" )
                .notificationSentAt( refinementLocalDateUtils.formatInstantToString( Instant.parse( SENT_AT_DATE ) ) )
                .noticeCode( "noticeCode" )
                .creditorTaxId( "creditorTaxId" )
                .debtorPosUpdateDate( formattedEventDate )
                .recordCreationDate( formattedEventDate )
                .recipientIdx( 0 )
                .version( 1 )
                .moreFields( AsseverationEvent.Payload.AsseverationMoreField.builder().build() )
                .build();

        assertDoesNotThrow(() -> (asseverationEventsProducer ).sendAsseverationEvent( asseverationEvent ));
        Mockito.verify(notificationCostServiceMonitor)
                .monitorNewNotificationPriceService(
                        internalNotification,
                        requestCaptor.getValue(),
                        notificationProcessCostResponseInt
                );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFailureForDownstreamProblem() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setNotificationFeePolicy( NotificationFeePolicy.FLAT_RATE );

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), anyBoolean() ) )
                .thenReturn( Optional.of( internalNotification ) );

        PnInternalException exception =  new PnInternalException("errore", "GENERIC_ERROR");

        Mockito.when( notificationCostServiceFactory.getNotificationCostServiceBySentAt(Mockito.any()))
                .thenReturn( notificationCostService );
        Mockito.when( notificationCostService.getNotificationCost(Mockito.any()))
                .thenThrow(exception);

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        assertThrows(PnInternalException.class, todo);

        Mockito.verify(notificationCostServiceMonitor, Mockito.never())
                .monitorNewNotificationPriceService(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceCostDaoFailure() {

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        assertThrows(PnNotFoundException.class, todo);

        Mockito.verify(notificationCostServiceMonitor, Mockito.never())
                .monitorNewNotificationPriceService(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceNotificationDaoFailure() {

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .build();

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), anyBoolean() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        assertThrows(PnNotFoundException.class, todo);

        Mockito.verify(notificationCostServiceMonitor, Mockito.never())
                .monitorNewNotificationPriceService(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostSuccess() {
        // Given
        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .build();

        // When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        NotificationCostResponse costResponse = svc.getNotificationCost( PA_TAX_ID, NOTICE_CODE );

        // Then
        Assertions.assertNotNull( costResponse );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostFailure() {
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationCost( PA_TAX_ID, NOTICE_CODE );

        assertThrows( PnNotFoundException.class, todo );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void removeAllNotificationCostsByIunTest() {
        String iun = "a-iun";
        String paTaxId = "creditor-tax-id";
        String noticeCode = "a-notice-code";
        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
        notificationPaymentInfo.setPagoPa(
                it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment.builder()
                        .noticeCode(noticeCode)
                        .creditorTaxId(paTaxId)
                        .build()
        );
        notification.setRecipients(List.of(new NotificationRecipient()
                .recipientType(PF)
                .payment(List.of(notificationPaymentInfo))));
        Mockito.when(notificationDao.getNotificationByIun(iun, false)).thenReturn(Optional.of(notification));

        assertDoesNotThrow(() -> svc.removeAllNotificationCostsByIun(iun));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void removeAllNotificationCostsByIunWithNotificationNotFoundTest() {
        String iun = "a-iun";
        String paTaxId = "creditor-tax-id";
        String noticeCode = "a-notice-code";
        NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
        notificationPaymentInfo.setPagoPa(
                it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment.builder()
                        .creditorTaxId(paTaxId)
                        .noticeCode(noticeCode).build()
        );
        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setRecipients(List.of(new NotificationRecipient()
                .recipientType(PF)
                .payment(List.of(notificationPaymentInfo))));
        Mockito.when(notificationDao.getNotificationByIun(iun, false)).thenReturn(Optional.empty());

        assertThrows(PnNotFoundException.class, () -> svc.removeAllNotificationCostsByIun(iun));
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPrice_shouldThrowWhenNoRecipientsOrPaymentsFound() {
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.FLAT_RATE);

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .recipientType( RecipientType.PF.getValue() )
                .recipientIdx(0)
                .build();

        internalNotification.getRecipients().get(0).getPayments().get(0)
                        .getPagoPa().setNoticeCode("invalidNoticeCode");

        Mockito.when(notificationDao.getNotificationByIun(Mockito.anyString(), anyBoolean()))
                .thenReturn(Optional.of(internalNotification));
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Assertions.assertThrows(PnNotificationNotFoundException.class, () -> svc.getNotificationPrice(PA_TAX_ID, NOTICE_CODE));

        Mockito.verify(notificationCostServiceMonitor, Mockito.never())
                .monitorNewNotificationPriceService(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any()
                );
    }

    @NotNull
    private InternalNotification getNewInternalNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setRecipientIds(List.of("0"));
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setPaFee(0);
        internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .payments(List.of(NotificationPaymentInfo.builder()
                                .pagoPa(it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment.builder()
                                        .noticeCode("noticeCode").build()).build()))
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }

}

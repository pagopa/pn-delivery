package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import it.pagopa.pn.delivery.models.InternalAsseverationEvent;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

class NotificationPriceServiceTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";
    public static final String REFINEMENT_DATE = "2022-10-07T11:01:25.122312Z";
    public static final String EXPECTED_REFINEMENT_DATE = "2022-10-07T23:59:59.999999999+02:00";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    public static final String SENT_AT_DATE = "2023-03-08T14:35:39.214793Z";
    public static final String EVENT_DATE = "2023-03-08T15:45:39.753534Z";

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;

    @Mock
    private NotificationMetadataEntityDao notificationMetadataEntityDao;

    @Mock
    private PnDeliveryPushClientImpl deliveryPushClient;

    @Mock
    private AsseverationEventsProducer asseverationEventsProducer;

    private final RefinementLocalDate refinementLocalDateUtils = new RefinementLocalDate();

    private NotificationPriceService svc;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed( Instant.parse( EVENT_DATE ), ZoneId.of("UTC"));
        svc = new NotificationPriceService(clock, notificationCostEntityDao, notificationDao, notificationMetadataEntityDao, deliveryPushClient, asseverationEventsProducer, refinementLocalDateUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setNotificationFeePolicy( NotificationFeePolicy.FLAT_RATE );

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxIdNoticeCode( "creditorTaxId##noticeCode" )
                .recipientType( RecipientType.PF.getValue() )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( notificationMetadataEntityDao.get( Mockito.any( Key.class ) ) ).thenReturn( Optional.of(NotificationMetadataEntity.builder()
                        .iunRecipientId( "iun##recipientInternalId0" )
                .build())
        );

        Mockito.when( deliveryPushClient.getNotificationProcessCost( Mockito.anyString(), Mockito.anyInt(), Mockito.any( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy.class ) ))
                .thenReturn( new NotificationProcessCostResponse()
                        .amount( 2000 )
                        .refinementDate( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE ) )
                );


        NotificationPriceResponse response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( 2000, response.getAmount() );
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

        Mockito.verify( asseverationEventsProducer ).sendAsseverationEvent( asseverationEvent );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFailure() {
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

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( notificationMetadataEntityDao.get( Mockito.any( Key.class ) ) ).thenReturn( Optional.empty() );


        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertThrows(PnNotFoundException.class, todo);
    }



    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceCostDaoFailure() {

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        Assertions.assertThrows(PnNotFoundException.class, todo);
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

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        Assertions.assertThrows(PnNotFoundException.class, todo);
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

        Assertions.assertThrows( PnNotFoundException.class, todo );
    }

    @NotNull
    private InternalNotification getNewInternalNotification() {
        return new InternalNotification(FullSentNotificationV11.builder()
                .notificationFeePolicy( NotificationFeePolicy.DELIVERY_MODE )
                .iun( "iun" )
                .sentAt( OffsetDateTime.parse(SENT_AT_DATE) )
                .senderPaId( "senderPaId" )
                .recipientIds(List.of( "recipientInternalId0" ))
                .sourceChannel(X_PAGOPA_PN_SRC_CH)
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .physicalAddress(NotificationPhysicalAddress.builder()
                                .foreignState( "Italia" )
                                .build())
                        .payment( NotificationPaymentInfo.builder()
                                .creditorTaxId( PA_TAX_ID )
                                .noticeCode( NOTICE_CODE )
                                .build() )
                        .build()) )
                .timeline( List.of( TimelineElementV11.builder()
                                .category( TimelineElementCategory.REFINEMENT )
                                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE ) )
                                .build(),
                        TimelineElementV11.builder()
                                .category( TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER )
                                .details( TimelineElementDetails.builder()
                                        .recIndex( 0 )
                                        .physicalAddress( PhysicalAddress.builder()
                                                .foreignState( "Italia" )
                                                .build() )
                                        .build() )
                                .build()) )
                .build());
    }
}

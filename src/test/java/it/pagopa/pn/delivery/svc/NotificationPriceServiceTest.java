package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
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

import java.time.OffsetDateTime;
import java.util.*;

class NotificationPriceServiceTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";
    public static final String REFINEMENT_DATE = "2022-10-07T11:01:25.122312Z";
    public static final String EXPECTED_REFINEMENT_DATE = "2022-10-07T23:59:59.999999999+02:00";

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;


    @Mock
    private PnDeliveryConfigs cfg;

    @Mock
    private NotificationRetrieverService retrieverService;

    private RefinementLocalDate refinementLocalDateUtils = new RefinementLocalDate();

    private NotificationPriceService svc;

    @BeforeEach
    void setup() {
        svc = new NotificationPriceService( notificationCostEntityDao, notificationDao, retrieverService, cfg, refinementLocalDateUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceDeliveryModeSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();

        PnDeliveryConfigs.Costs costs = new PnDeliveryConfigs.Costs();
        costs.setNotification( "200" );
        costs.setRaccomandataIta( "540" );
        costs.setRaccomandataEstZona1( "710" );

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );

        Mockito.when( cfg.getCosts() ).thenReturn( costs );

        NotificationPriceResponse response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( "740", response.getAmount() );
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE ) , response.getEffectiveDate() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFlatRateSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setNotificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE );


        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );


        NotificationPriceResponse response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( "0", response.getAmount() );
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE ) , response.getEffectiveDate() );
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
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
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
    void getNotificationPriceNoTimelineRefinementSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotificationNoRefinement();

        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(internalNotificationCost) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );


        NotificationPriceResponse priceResponse = svc.getNotificationPrice( PA_TAX_ID, NOTICE_CODE );

        //Then
        Assertions.assertNotNull( priceResponse );
        Assertions.assertNull( priceResponse.getEffectiveDate() );
        Assertions.assertEquals( "0", priceResponse.getAmount() );

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationCostSuccess() {
        // Given
        InternalNotificationCost internalNotificationCost = InternalNotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
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
        return new InternalNotification(FullSentNotification.builder()
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.DELIVERY_MODE )
                .iun( "iun" )
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
                .timeline( List.of( TimelineElement.builder()
                                .category( TimelineElementCategory.REFINEMENT )
                                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE ) )
                                .build(),
                        TimelineElement.builder()
                                .category( TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER )
                                .details( TimelineElementDetails.builder()
                                        .recIndex( 0 )
                                        .physicalAddress( PhysicalAddress.builder()
                                                .foreignState( "Italia" )
                                                .build() )
                                        .build() )
                                .build()) )
                .build(), Collections.emptyMap(), Collections.emptyList() );
    }

    @NotNull
    private InternalNotification getNewInternalNotificationNoRefinement() {
        return new InternalNotification(FullSentNotification.builder()
                .iun( "iun" )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
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
                .timeline( List.of( TimelineElement.builder()
                                .category( TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER )
                                .details( TimelineElementDetails.builder()
                                        .recIndex( 0 )
                                        .physicalAddress( PhysicalAddress.builder()
                                                .foreignState( "Italia" )
                                                .build() )
                                        .build() )
                                .build()) )
                .build(), Collections.emptyMap(), Collections.emptyList() );
    }



}
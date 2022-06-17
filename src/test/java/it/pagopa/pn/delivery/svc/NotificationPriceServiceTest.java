package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationCost;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.jetbrains.annotations.NotNull;
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

class NotificationPriceServiceTest {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_NUMBER = "noticeNumber";

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private NotificationCostEntityDao notificationCostEntityDao;


    @Mock
    private PnDeliveryConfigs cfg;

    @Mock
    private NotificationRetrieverService retrieverService;

    private NotificationPriceService svc;

    @BeforeEach
    void setup() {
        svc = new NotificationPriceService( notificationCostEntityDao, notificationDao, retrieverService, cfg );
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

        NotificationCost notificationCost = NotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of( notificationCost ) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );

        Mockito.when( cfg.getCosts() ).thenReturn( costs );

        NotificationPriceResponse response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_NUMBER );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( "740", response.getAmount() );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceFlatRateSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();
        internalNotification.setNotificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE );


        NotificationCost notificationCost = NotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of( notificationCost ) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );


        NotificationPriceResponse response = svc.getNotificationPrice( PA_TAX_ID, NOTICE_NUMBER );

        //Then
        Assertions.assertNotNull( response );
        Assertions.assertEquals("iun" , response.getIun() );
        Assertions.assertEquals( "0", response.getAmount() );
    }



    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceCostDaoFailure() {

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_NUMBER );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceNotificationDaoFailure() {

        NotificationCost notificationCost = NotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of( notificationCost ) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.empty() );

        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_NUMBER );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getNotificationPriceNoTimelineRefinementFailure() {
        //Given
        InternalNotification internalNotification = getNewInternalNotificationNoRefinement();

        NotificationCost notificationCost = NotificationCost.builder()
                .recipientIdx( 0 )
                .iun( "iun" )
                .creditorTaxId_noticeCode( "creditorTaxId##noticeCode" )
                .build();

        //When
        Mockito.when( notificationCostEntityDao.getNotificationByPaymentInfo( Mockito.anyString(),Mockito.anyString() ) )
                .thenReturn( Optional.of(  notificationCost  ) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) )
                .thenReturn( Optional.of( internalNotification ) );

        Mockito.when( retrieverService.enrichWithTimelineAndStatusHistory( Mockito.anyString(), Mockito.any( InternalNotification.class ) ) )
                .thenReturn( internalNotification );


        Executable todo = () -> svc.getNotificationPrice( PA_TAX_ID, NOTICE_NUMBER );

        //Then
        Assertions.assertThrows(PnNotFoundException.class, todo);

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
                                .noticeCode( NOTICE_NUMBER )
                                .build() )
                        .build()) )
                .timeline( List.of( TimelineElement.builder()
                                .category( TimelineElementCategory.REFINEMENT )
                                .timestamp( Date.from( Instant.now() ) )
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
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .physicalAddress(NotificationPhysicalAddress.builder()
                                .foreignState( "Italia" )
                                .build())
                        .payment( NotificationPaymentInfo.builder()
                                .creditorTaxId( PA_TAX_ID )
                                .noticeCode( NOTICE_NUMBER )
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
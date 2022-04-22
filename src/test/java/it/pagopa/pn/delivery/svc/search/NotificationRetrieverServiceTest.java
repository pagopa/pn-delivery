package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClient;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

class NotificationRetrieverServiceTest {

    private Clock clock;
    private FileStorage fileStorage;
    private S3PresignedUrlService presignedUrlService;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClient pnDeliveryPushClient;
    private StatusUtils statusUtils;
    private PnMandateClientImpl pnMandateClient;
    private PnDeliveryConfigs cfg;

    private NotificationRetrieverService svc;


    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.fileStorage = Mockito.mock(FileStorage.class);
        this.presignedUrlService = Mockito.mock(S3PresignedUrlService.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClient.class);
        this.statusUtils = Mockito.mock(StatusUtils.class);
        this.cfg = Mockito.mock(PnDeliveryConfigs.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.svc = new NotificationRetrieverService(clock,
                fileStorage,
                presignedUrlService,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                statusUtils,
                cfg,
                pnMandateClient
        );
    }

    @Test
    void checkMandateFailure() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-03-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-04-30T00:00:00.00Z" ) )
                .senderReceiverId( "receiverId" )
                .size( 10 )
                .nextPagesKey( null )
                .build();
        //When
        Executable todo = () -> svc.searchNotification( inputSearch, "senderId" );
        //Then
        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @Test
    void checkMandateSuccess() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-03-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-04-30T00:00:00.00Z" ) )
                .senderReceiverId( "receiverId" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate( "senderId" );
        internalMandateDto.setDelegator( "receiverId" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        //When
        Mockito.when( pnMandateClient.getMandates( Mockito.anyString() ) ).thenReturn( mandateResult );

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch, "senderId" );

        Assertions.assertNotNull( result );

    }

    @Test
    void checkMandateNoValidMandate() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-03-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-04-30T00:00:00.00Z" ) )
                .senderReceiverId( "receiverId" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate( "senderId" );
        internalMandateDto.setDelegator( "asdasd" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        //When
        Mockito.when( pnMandateClient.getMandates( Mockito.anyString() ) ).thenReturn( mandateResult );

        Executable todo = () -> svc.searchNotification( inputSearch, "senderId" );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }
}
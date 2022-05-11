package it.pagopa.pn.delivery.svc.search;



import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.api.dto.notification.timeline.NotificationHistoryResponse;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClient;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

class NotificationRetrieverServiceTest {

    private static final String IUN = "iun";
    private static final String USER_ID = "userId";
    private static final String FAKE_URL = "fake_url";

    private Clock clock;
    private FileStorage fileStorage;
    private S3PresignedUrlService presignedUrlService;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClient pnDeliveryPushClient;
    private PnMandateClientImpl pnMandateClient;
    private PnDeliveryConfigs cfg;
    private ModelMapperFactory modelMapperFactory;

    private NotificationRetrieverService svc;


    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.fileStorage = Mockito.mock(FileStorage.class);
        this.presignedUrlService = Mockito.mock(S3PresignedUrlService.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClient.class);
        this.cfg = Mockito.mock(PnDeliveryConfigs.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.modelMapperFactory = Mockito.mock(ModelMapperFactory.class);
        this.svc = new NotificationRetrieverService(clock,
                fileStorage,
                presignedUrlService,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                cfg,
                pnMandateClient,
                modelMapperFactory);
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
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( mandateResult );

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch );

        Assertions.assertNotNull( result );

    }

    @Test
    void checkMandateNoValidMandate() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-03-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-04-30T00:00:00.00Z" ) )
                .senderReceiverId( "receiverId" )
                .mandateId( "mandateId" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate( "senderId" );
        internalMandateDto.setDelegator( "asdasd" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        //When
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( mandateResult );

        Executable todo = () -> svc.searchNotification( inputSearch );

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }
    
    @Test
    void getNotificationWithTimelineInfoSuccess() {
        //Given
        InternalNotification notification = new InternalNotification(FullSentNotification.builder()
                .iun( IUN )
                .sentAt(Date.from(Instant.now()))
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .build())
                ).build(), Collections.EMPTY_MAP);

        
        Set<it.pagopa.pn.api.dto.notification.timeline.TimelineElement> tle = Collections.singleton( it.pagopa.pn.api.dto.notification.timeline.TimelineElement.builder()
                .iun( IUN )
                .elementId( "elementId" )
                .category(it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory.REQUEST_ACCEPTED )
                .timestamp( Instant.now() )
                .build());

        NotificationHistoryResponse timelineStatusHistoryDto = NotificationHistoryResponse.builder()
                .timelineElements(tle)
                .statusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                                .status(NotificationStatus.ACCEPTED)
                        .build() ) )
                .build();
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.api.dto.notification.timeline.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.api.dto.notification.timeline.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.api.dto.notification.status.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperStatus = new ModelMapper();
        mapperStatus.createTypeMap( it.pagopa.pn.api.dto.notification.status.NotificationStatus.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.api.dto.notification.status.NotificationStatus.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus.class ) )
                .thenReturn( mapperStatus );

        InternalNotification result = svc.getNotificationInformation( IUN );
        
        //Then
        Assertions.assertNotNull( result );
    }

    @Test
    void getNotificationWithTimelineInfoError() {
        //Given

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.empty() );
        Executable todo = () -> svc.getNotificationInformation( IUN );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

/*    @Test
    void getNotificationAndViewEventSuccess() {
        //Given
        InternalNotification notification = InternalNotification.builder()
                .sentAt(Date.from(Instant.now()))
                .iun( IUN )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );

        Set<TimelineElement> tle = Collections.singleton( TimelineElement.builder()
                .iun( IUN )
                .elementId( "elementId" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .timestamp( Instant.now() )
                .build());

        NotificationHistoryResponse timelineStatusHistoryDto = NotificationHistoryResponse.builder()
                .timelineElements(tle)
                .build();
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class) ) ).thenReturn( timelineStatusHistoryDto );
        
        svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
    }

    @Test
    void getNotificationAndViewEventError() {
        //Given
        Notification notification = Notification.builder()
                .sentAt(Instant.now())
                .iun( IUN )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );

        Set<TimelineElement> tle = Collections.singleton( TimelineElement.builder()
                .iun( IUN )
                .elementId( "elementId" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .timestamp( Instant.now() )
                .build());

        NotificationHistoryResponse timelineStatusHistoryDto = NotificationHistoryResponse.builder()
                .timelineElements(tle)
                .build();
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class) ) ).thenReturn( timelineStatusHistoryDto );

        Executable todo = () -> svc.getNotificationAndNotifyViewedEvent( IUN, "" );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void downloadDocumentSuccess() {
        //Given
        Notification notification = Notification.builder()
                .iun( IUN )
                .documents(List.of( NotificationAttachment.builder()
                                .ref( NotificationAttachment.Ref.builder().build() )
                        .build() ))
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        svc.downloadDocument( IUN, 0 );

        //Then
        Mockito.verify( fileStorage ).loadAttachment( NotificationAttachment.Ref.builder().build() );
    }

    @Test
    void downloadDocumentError() {
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) ).thenReturn( Optional.empty() );
        Executable todo = () -> svc.downloadDocument( IUN, 0 );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void downloadDocumentWithRedirect() {
        //Given
        Notification notification = Notification.builder()
                .iun( IUN )
                .documents(List.of( NotificationAttachment.builder()
                        .ref( NotificationAttachment.Ref.builder().build() )
                        .build() ))
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();
        PreloadResponse response = PreloadResponse.builder()
                .url( FAKE_URL )
                .build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( presignedUrlService.presignedDownload( Mockito.anyString(), Mockito.any( NotificationAttachment.class ) ) ).thenReturn( response );
        svc.downloadDocumentWithRedirect( IUN, 0 );

        //Then
        Mockito.verify( presignedUrlService ).presignedDownload( IUN + "doc_0", NotificationAttachment.builder()
                .ref( NotificationAttachment.Ref.builder().build() )
                .build() );
    }*/

    @Test
    void downloadDocumentWithRedirectError() {
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) ).thenReturn( Optional.empty() );
        Executable todo = () -> svc.downloadDocumentWithRedirect( IUN, 0 );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }
}

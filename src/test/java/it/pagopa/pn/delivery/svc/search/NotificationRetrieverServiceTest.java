package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationRecipientType;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    
    @Test
    void getNotificationWithTimelineInfoSuccess() {
        //Given
        Notification notification = Notification.builder()
                .iun( IUN )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .build())
                ).build();

        Set<TimelineElement> tle = Collections.singleton( TimelineElement.builder()
                .iun( IUN )
                .elementId( "elementId" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .timestamp( Instant.now() )
                .build());
        
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineElements( Mockito.anyString() ) ).thenReturn( tle );
        Notification result = svc.getNotificationInformation( IUN );
        
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

    @Test
    void getNotificationAndViewEventSuccess() {
        //Given
        Notification notification = Notification.builder()
                .iun( IUN )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
    }

    @Test
    void getNotificationAndViewEventError() {
        //Given
        Notification notification = Notification.builder()
                .iun( IUN )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipientType.PF )
                        .taxId( USER_ID )
                        .build())
                ).build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
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
    }

    @Test
    void downloadDocumentWithRedirectError() {
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) ).thenReturn( Optional.empty() );
        Executable todo = () -> svc.downloadDocumentWithRedirect( IUN, 0 );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }
}

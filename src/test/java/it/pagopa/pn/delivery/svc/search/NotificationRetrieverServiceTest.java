package it.pagopa.pn.delivery.svc.search;



import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.time.*;
import java.util.*;

class NotificationRetrieverServiceTest {

    private static final String IUN = "iun";
    private static final String USER_ID = "userId";
    private static final String MANDATE_ID = "mandateId";

    private Clock clock;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private PnMandateClientImpl pnMandateClient;
    private PnDataVaultClientImpl dataVaultClient;
    private PnDeliveryConfigs cfg;
    private ModelMapperFactory modelMapperFactory;

    private NotificationRetrieverService svc;


    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClientImpl.class);
        this.cfg = Mockito.mock(PnDeliveryConfigs.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.dataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        this.modelMapperFactory = Mockito.mock(ModelMapperFactory.class);
        this.svc = new NotificationRetrieverService(clock,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                cfg,
                pnMandateClient,
                dataVaultClient,
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
        InternalNotification notification = getNewInternalNotification();


        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                //.iun( IUN )
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REQUEST_ACCEPTED )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification result = svc.getNotificationInformation( IUN );
        
        //Then
        Assertions.assertNotNull( result );
    }

    @NotNull
    private InternalNotification getNewInternalNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun( IUN )
                .sentAt(Date.from(Instant.now()))
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .payment( NotificationPaymentInfo.builder()
                                .creditorTaxId( "77777777777" )
                                .noticeCode( "302000100000019421" )
                                .creditorTaxIdOptional( "77777777778" )
                                .noticeCodeOptional( "302000100000019422" )
                                .build() )
                        .build())
                ).build(), Collections.emptyMap(), Collections.singletonList( "userId" ) );
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
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventByDelegateSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID );
        internalMandateDto.setDelegate( "senderId" );
        internalMandateDto.setDelegator( "userId" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        //When
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( mandateResult );
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, MANDATE_ID );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventDocsUnavSuccess() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp( OffsetDateTime.parse( "2022-01-01T00:00:00.00Z" ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );


        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventError() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        Executable todo = () -> svc.getNotificationAndNotifyViewedEvent( IUN, "", null );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void getNotificationWith2IUV() {
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-06-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperTimeline = new ModelMapper();
        mapperTimeline.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement.class, TimelineElement.class ) )
                .thenReturn( mapperTimeline );

        ModelMapper mapperStatusHistory = new ModelMapper();
        mapperStatusHistory.createTypeMap( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class );
        Mockito.when( modelMapperFactory.createModelMapper( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement.class, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElement.class ) )
                .thenReturn( mapperStatusHistory );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, null, 0 );
    }

    /*@Test
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
    */

}

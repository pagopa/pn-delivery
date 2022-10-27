package it.pagopa.pn.delivery.svc.search;



import it.pagopa.pn.commons.configs.IsMVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.externalregistries.model.PaymentStatus;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
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
    private static final String REQUEST_ID = "aXVu";
    private static final String USER_ID = "userId";
    private static final String SENDER_ID = "senderId";
    private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
    private static final String IDEMPOTENCE_TOKEN = "idempotenceToken";
    private static final String MANDATE_ID = "mandateId";
    public static final String NOTICE_CODE = "302000100000019421";
    public static final String NOTICE_CODE_ALTERNATIVE = "302000100000019422";
    public static final String SENDER_TAXID = "01199250158";

    private Clock clock;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private PnMandateClientImpl pnMandateClient;
    private PnDataVaultClientImpl dataVaultClient;
    private PnExternalRegistriesClientImpl externalRegistriesClient;
    private ModelMapperFactory modelMapperFactory;

    private NotificationRetrieverService svc;

    private NotificationSearchFactory notificationSearchFactory;
    private NotificationSearch notificationSearch;
    private RefinementLocalDate refinementLocalDateUtils;
    private IsMVPParameterConsumer isMVPParameterConsumer;
    private PnDeliveryConfigs cfg;

    @BeforeEach
    void setup() {
        this.clock = Mockito.mock(Clock.class);
        this.notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
        this.notificationDao = Mockito.mock(NotificationDao.class);
        this.pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClientImpl.class);
        this.pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
        this.dataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        this.externalRegistriesClient = Mockito.mock( PnExternalRegistriesClientImpl.class );
        this.modelMapperFactory = Mockito.mock(ModelMapperFactory.class);
        this.notificationSearchFactory = Mockito.mock(NotificationSearchFactory.class);
        this.notificationSearch = Mockito.mock(NotificationSearch.class);
        this.refinementLocalDateUtils = new RefinementLocalDate();
        this.isMVPParameterConsumer = Mockito.mock( IsMVPParameterConsumer.class );
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );

        Mockito.when(notificationSearchFactory.getMultiPageSearch(Mockito.any(), Mockito.any())).thenReturn(notificationSearch);
        Mockito.when( cfg.getMaxDocumentsAvailableDays() ).thenReturn( "120" );
        Mockito.when( cfg.getMaxFirstNoticeCodeDays() ).thenReturn( "5" );
        Mockito.when( cfg.getMaxSecondNoticeCodeDays() ).thenReturn( "60" );

        this.svc = new NotificationRetrieverService(
                clock,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                pnMandateClient,
                dataVaultClient,
                externalRegistriesClient,
                modelMapperFactory,
                notificationSearchFactory,
                refinementLocalDateUtils,
                isMVPParameterConsumer,
                cfg);
    }

    @Test
    void checkMachingGroups() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "SENDER_ID" )
                .size( 10 )
                .nextPagesKey( null )
                .build();
        List<PaGroup> groups = getGroups();
        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);
        Mockito.when( externalRegistriesClient.getGroups( Mockito.anyString() )).thenReturn( groups );

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch );

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( "Group", results.getResultsPage().get(0).getGroup());
        Assertions.assertEquals( "Group no match", results.getResultsPage().get(1).getGroup());
        Assertions.assertEquals( "group-code-fake", results.getResultsPage().get(2).getGroup());
    }

    @NotNull
    private ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> getPaginatedNotifications() {
        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = new ResultPaginationDto<>();
        List<NotificationSearchRow> notifications = new ArrayList<>();
        // first notification
        NotificationSearchRow notification = new NotificationSearchRow();
        notification.setIun("first-iun");
        notification.setPaProtocolNumber("first-protocol-number");
        notification.setGroup("group-code");
        notifications.add(notification);
        // second notification
        notification = new NotificationSearchRow();
        notification.setIun("second-iun");
        notification.setPaProtocolNumber("second-protocol-number");
        notification.setGroup("group-code-no-match");
        notifications.add(notification);
        // third notification
        notification = new NotificationSearchRow();
        notification.setIun("third-iun");
        notification.setPaProtocolNumber("third-protocol-number");
        notification.setGroup("group-code-fake");
        results.setResultsPage(notifications);
        notifications.add(notification);
        return  results;
    }

    @Test
    void checkMandateSuccess() {
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
        internalMandateDto.setMandateId( "mandateId" );
        internalMandateDto.setDelegate( "senderId" );
        internalMandateDto.setDelegator( "receiverId" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );
        internalMandateDto.setDateto( "2022-05-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(new ResultPaginationDto<>());

        //When
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( mandateResult );

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch );

        Assertions.assertNotNull( result );

    }

    @Test
    void checkMandateNoValidMandate() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
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
    void searchValidateInputException() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto(
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                true,
                true,
                null
        );

        Executable todo = () -> svc.searchNotification( inputSearch );

        Assertions.assertThrows( PnValidationException.class, todo );
    }

    @Test
    void searchNotificationUnableDeserializeLEK() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto.Builder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "senderId" )
                .size( 10 )
                .nextPagesKey( "fakeNextPageKey" )
                .build();

        Executable todo = () -> svc.searchNotification( inputSearch );

        Assertions.assertThrows( PnInternalException.class, todo );
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
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification result = svc.getNotificationInformation( IUN );
        
        //Then
        Assertions.assertNotNull( result );
        Assertions.assertNull( result.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
    }

    @Test
    void getNotificationInformationByProtocolAndIdempotenceSuccess() {
        // Given
        InternalNotification notification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
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

        // When
        Mockito.when( notificationDao.getRequestId( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ) ).thenReturn( Optional.of( REQUEST_ID ) );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification result = svc.getNotificationInformation( SENDER_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN );

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( IUN, result.getIun() );
        Assertions.assertEquals( PA_PROTOCOL_NUMBER, result.getPaProtocolNumber() );
        Assertions.assertEquals( IDEMPOTENCE_TOKEN, result.getIdempotenceToken() );
    }

    @Test
    void getNotificationInformationByProtocolAndIdempotenceFailure() {
        // When
        Mockito.when( notificationDao.getRequestId( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ) ).thenReturn( Optional.empty() );
        Executable todo = () -> svc.getNotificationInformation( SENDER_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN );

        // Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void getNotificationInfoReturnFirstNoticeCode() {
        InternalNotification notification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
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

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ) ).thenReturn( Optional.of( notification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );

        InternalNotification result = svc.getNotificationInformation( IUN, true, false );

        Assertions.assertNotNull( result );
    }

    @NotNull
    private InternalNotification getNewInternalNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun( IUN )
                .idempotenceToken( IDEMPOTENCE_TOKEN )
                .paProtocolNumber( PA_PROTOCOL_NUMBER )
                .sentAt( OffsetDateTime.now() )
                .senderTaxId( SENDER_TAXID )
                .recipients(Collections.singletonList(NotificationRecipient.builder()
                        .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                        .payment( NotificationPaymentInfo.builder()
                                .creditorTaxId( "77777777777" )
                                .noticeCode( NOTICE_CODE )
                                .noticeCodeAlternative( NOTICE_CODE_ALTERNATIVE )
                                .build() )
                        .build())
                ).build(), Collections.emptyMap(), Collections.singletonList( "userId" ) );
    }

    @NotNull
    private List<PaGroup> getGroups() {
        List<PaGroup> groups = new ArrayList<>();
        // first group
        PaGroup dto = new PaGroup();
        dto.setId("group-code");
        dto.setName("Group");
        groups.add(dto);
        // second group
        dto = new PaGroup();
        dto.setId("group-code-no-match");
        dto.setName("Group no match");
        groups.add(dto);
        return groups;
    }

    @Test
    void getNotificationWithTimelineInfoError() {
        //Given

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.empty() );
        Executable todo = () -> svc.getNotificationInformation( IUN );

        //Then
        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @Test
    void getNotificationWithMatchingGroup() {
        //Given
        InternalNotification notification = getNewInternalNotification();
        notification.setGroup("group-code");
        List<PaGroup> groups = getGroups();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( externalRegistriesClient.getGroups( Mockito.anyString() )).thenReturn( groups );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification result = svc.getNotificationInformation( IUN, false, true, "SENDER_ID");

        //Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals("Group", result.getGroup());
    }

    @Test
    void getNotificationWithNoMatchingGroup() {
        //Given
        InternalNotification notification = getNewInternalNotification();
        notification.setGroup("group-code-fake");
        List<PaGroup> groups = getGroups();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( notification ) );
        Mockito.when( externalRegistriesClient.getGroups( Mockito.anyString() )).thenReturn( groups );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification result = svc.getNotificationInformation( IUN, false, true, "SENDER_ID");

        //Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals("group-code-fake", result.getGroup());
    }

    @Test
    void checkRefinementDateOraSolare() {
        // Given
        List<TimelineElement> timelineElementList = List.of( TimelineElement.builder()
                        .category( TimelineElementCategory.REFINEMENT )
                        .timestamp( OffsetDateTime.parse( "2022-10-05T12:23:15.123456Z" ) )
                .build(),
                TimelineElement.builder()
                        .category( TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp( OffsetDateTime.parse( "2022-10-03T10:10:15.123456Z" ) )
                        .build()
        );

        // When
        OffsetDateTime refinementDate = svc.findRefinementDate(timelineElementList, IUN );

        // Then
        OffsetDateTime expectedRefinementDate = OffsetDateTime.parse( "2022-10-03T23:59:59.999999999+02:00" );
        Assertions.assertEquals( expectedRefinementDate, refinementDate );
    }

    @Test
    void checkRefinementDateOraLegale() {
        // Given
        List<TimelineElement> timelineElementList = List.of( TimelineElement.builder()
                        .category( TimelineElementCategory.REFINEMENT )
                        .timestamp( OffsetDateTime.parse( "2022-12-05T12:23:15.123456Z" ) )
                        .build(),
                TimelineElement.builder()
                        .category( TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp( OffsetDateTime.parse( "2022-12-03T10:10:15.123456Z" ) )
                        .build()
        );

        // When
        OffsetDateTime refinementDate = svc.findRefinementDate(timelineElementList, IUN );

        // Then
        OffsetDateTime expectedRefinementDate = OffsetDateTime.parse( "2022-12-03T23:59:59.999999999+01:00" );
        Assertions.assertEquals( expectedRefinementDate, refinementDate );
    }

    @Test
    void getNotificationAndViewEventSuccess() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
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
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventByDelegateSuccess() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
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
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, MANDATE_ID );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventDocsUnavSuccess() {
        //Given
        String nowTestInstant = "2022-06-30T00:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp( OffsetDateTime.parse( "2022-03-01T12:00:00.00Z" ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );


        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
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
        Mockito.when( clock.instant() ).thenReturn( Instant.now() );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        Executable todo = () -> svc.getNotificationAndNotifyViewedEvent( IUN, "", null );

        //Then
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void getNotificationWith2IUVBeforeTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-01-15T00:00:00.00Z" ) );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-01-15T00:00:00.00Z" ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
        Assertions.assertEquals( NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );

    }

    @Test
    void getNotificationWith2IUVWithFirstIUVPayedAfterTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.SUCCEEDED );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-02-11T00:00:00.00Z" ) );
        Mockito.when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-02-11T00:00:00.00Z" ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
        Assertions.assertEquals( NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
    }

    @Test
    void getNotificationWith2IUVWithFirstIUVNoPayedAfterTerms() {
        String nowTestInstant = "2022-01-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
        Assertions.assertEquals( NOTICE_CODE_ALTERNATIVE, internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
    }

    @Test
    void getNotificationWith2IUVFirstLimitDay() {
        String nowTestInstant = "2022-03-06T23:59:00.00Z";

        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-02T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
        Assertions.assertEquals( NOTICE_CODE ,internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
    }

    @Test
    void getNotificationWith2IUVLastLimitDay() {
        String nowTestInstant = "2022-05-01T15:00:00.00Z";

        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-02T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
    }

    @Test
    void getNotificationWith2IUVAfterMaxTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-05-11T00:00:00.00Z" ) );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-05-11T00:00:00.00Z" ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
    }

    @Test
    void getNotificationWith2IUVWithExtRegistryException() {
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( isMVPParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( true );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-03-11T00:00:00.00Z" ) );
        Mockito.when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenThrow( PnHttpResponseException.class );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );
        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-03-11T00:00:00.00Z" ), 0 );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCode() );
        Assertions.assertNull( internalNotificationResult.getRecipients().get( 0 ).getPayment().getNoticeCodeAlternative() );
    }

    @Test
    void getNotificationWithoutDocumentsSuccess() {
        // Given
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.CANCELLED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.CANCELLED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        // Then
        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertEquals( Collections.emptyList(), internalNotification.getDocuments() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getPagoPaForm() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getF24flatRate() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getF24standard() );
    }

    @Test
    void getNotificationWithDocumentsLimitDay() {
        // Given
        String nowTestInstant = "2022-06-29T18:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T17:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        // Then
        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationWithoutDocumentsLimitDay() {
        // Given
        String nowTestInstant = "2022-06-30T00:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        List<it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement> tle = List.of( new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElement()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.TimelineElementCategory.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T17:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatusHistoryElement()
                        .status(it.pagopa.pn.delivery.generated.openapi.clients.deliverypush.model.NotificationStatus.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        ModelMapper mapperNotification = new ModelMapper();
        mapperNotification.createTypeMap( InternalNotification.class, FullSentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) )
                .thenReturn( mapperNotification );

        // Then
        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, USER_ID, null );

        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertEquals( Collections.emptyList(), internalNotification.getDocuments() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getPagoPaForm() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getF24flatRate() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayment().getF24standard() );

    }

}

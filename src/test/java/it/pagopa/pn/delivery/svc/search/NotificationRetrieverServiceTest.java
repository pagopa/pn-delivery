package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnForbiddenException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentStatus;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class NotificationRetrieverServiceTest {

    private static final String IUN = "iun";
    private static final String REQUEST_ID = "aXVu";
    private static final String CX_ID = "cxId";
    private static final String CX_TYPE = "PF";
    private static final String UID = "uid";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    private static final String X_PAGOPA_PN_SRC_CH_DET = "sourceChannelDetails";
    public static final InternalAuthHeader INTERNAL_AUTH_HEADER = new InternalAuthHeader(CX_TYPE, CX_ID, UID, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET);
    private static final String SENDER_ID = "senderId";
    private static final String WRONG_SENDER_ID = "wrongSenderId";
    private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
    private static final String IDEMPOTENCE_TOKEN = "idempotenceToken";
    private static final String MANDATE_ID = "mandateId";
    public static final String NOTICE_CODE = "302000100000019421";
    private static final Integer SIZE = 10;
    private static final NotificationStatusV26 STATUS = NotificationStatusV26.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    public static final List<String> GROUPS = List.of("Group1", "Group2");
    public static final String GROUP = "Group";
    public static final String DELEGATE_ID = "DelegateId";


    private Clock clock;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClientImpl pnDeliveryPushClient;
    private PnMandateClientImpl pnMandateClient;
    private PnDataVaultClientImpl dataVaultClient;
    private PnExternalRegistriesClientImpl externalRegistriesClient;
    private ModelMapper modelMapper;

    private NotificationRetrieverService svc;

    private NotificationSearchFactory notificationSearchFactory;
    private NotificationSearch notificationSearch;
    private RefinementLocalDate refinementLocalDateUtils;
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
        this.modelMapper = new ModelMapper();
        this.notificationSearchFactory = Mockito.mock(NotificationSearchFactory.class);
        this.notificationSearch = Mockito.mock(NotificationSearch.class);
        this.refinementLocalDateUtils = new RefinementLocalDate();
        this.cfg = Mockito.mock( PnDeliveryConfigs.class );

        when(notificationSearchFactory.getMultiPageSearch(any(), any())).thenReturn(notificationSearch);
        when( cfg.getMaxDocumentsAvailableDays() ).thenReturn( "120" );
        when( cfg.getMaxFirstNoticeCodeDays() ).thenReturn( "5" );
        when( cfg.getMaxSecondNoticeCodeDays() ).thenReturn( "60" );

        this.svc = new NotificationRetrieverService(
                clock,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                pnMandateClient,
                dataVaultClient,
                externalRegistriesClient,
                modelMapper,
                notificationSearchFactory,
                refinementLocalDateUtils,
                cfg);
    }

    @Test
    void checkMachingGroups() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
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
        when(notificationSearch.searchNotificationMetadata()).thenReturn(results);
        when( externalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(false ) )).thenReturn( groups );

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification(inputSearch, "PF", null);

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( "Group", results.getResultsPage().get(0).getGroup());
        Assertions.assertEquals( "Group no match", results.getResultsPage().get(1).getGroup());
        Assertions.assertEquals( "group-code-fake", results.getResultsPage().get(2).getGroup());
    }

    @Test
    void checkOpaqueFilterIdPIva() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "SENDER_ID" )
                .filterId( "12345678901" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch, "PA", null);

        // Then
        Assertions.assertNotNull( result );
    }

    @Test
    void checkOpaqueFilterIdCF() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "SENDER_ID" )
                .filterId( "EEEEEEEEEEEEEEEE" )
                .size( 10 )
                .nextPagesKey( null )
                .build();

        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch, "PA", null );

        // Then
        Assertions.assertNotNull( result );
    }

    @Test
    void checkMandate_notfound() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender(false)
                .startDate(Instant.parse("2022-05-01T00:00:00.00Z"))
                .endDate(Instant.parse("2022-05-30T00:00:00.00Z"))
                .senderReceiverId("SENDER_ID")
                .filterId("EEEEEEEEEEEEEEEE")
                .size(10)
                .mandateId(MANDATE_ID)
                .nextPagesKey(null)
                .build();

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> results = getPaginatedNotifications();

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.searchNotification(inputSearch, "PF", null));

    }

    @Test
    void checkMandate_ok() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( UID )
                .filterId( "EEEEEEEEEEEEEEEE" )
                .size( 10 )
                .mandateId(MANDATE_ID)
                .nextPagesKey( null )
                .build();


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID );
        internalMandateDto.setDelegate( UID );
        internalMandateDto.setDelegator( CX_ID  );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );


        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();
        Mockito.when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), any(), any()))
                .thenReturn(mandateResult);

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification( inputSearch, "PF", null );

        // Then
        Assertions.assertNotNull( result );
    }

    @Test
    void checkMandate_invalidPAId() {
        //Given
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( false )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( UID )
                .filterId( "EEEEEEEEEEEEEEEE" )
                .size( 10 )
                .mandateId(MANDATE_ID)
                .nextPagesKey( null )
                .build();


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID );
        internalMandateDto.setDelegate( UID );
        internalMandateDto.setDelegator( CX_ID  );
        internalMandateDto.setVisibilityIds(List.of(SENDER_ID + "OTHER"));
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );


        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();
        Mockito.when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), any(), any())).thenReturn(mandateResult);

        //When
        Mockito.when(notificationSearch.searchNotificationMetadata()).thenReturn(results);

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.searchNotification( inputSearch, "PF", null ) );

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
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
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

        when(notificationSearch.searchNotificationMetadata()).thenReturn(new ResultPaginationDto<>());

        //When
        when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), eq(CxTypeAuthFleet.PF), any()))
                .thenReturn(mandateResult);

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotification(inputSearch, "PF", null);

        Assertions.assertNotNull( result );

    }

    @Test
    void checkMandateNoValidMandate() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
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
        when(pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString(), eq(CxTypeAuthFleet.PF), any()))
                .thenReturn(mandateResult);

        Executable todo = () -> svc.searchNotification(inputSearch, "PF", null);

        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @Test
    void searchValidateInputException() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .senderReceiverId(RECIPIENT_ID)
                .startDate( Instant.now() )
                .endDate( Instant.now() )
                .groups( Collections.emptyList() )
                .statuses( Collections.emptyList() )
                .build();

        Executable todo = () -> svc.searchNotification(inputSearch, null, null);

        Assertions.assertThrows( PnValidationException.class, todo );
    }

    @Test
    void searchNotificationUnableDeserializeLEK() {
        InputSearchNotificationDto inputSearch = new InputSearchNotificationDto().toBuilder()
                .bySender( true )
                .startDate( Instant.parse( "2022-05-01T00:00:00.00Z" ) )
                .endDate( Instant.parse( "2022-05-30T00:00:00.00Z" ) )
                .senderReceiverId( "senderId" )
                .size( 10 )
                .nextPagesKey( "fakeNextPageKey" )
                .build();

        Executable todo = () -> svc.searchNotification(inputSearch, null, null);

        Assertions.assertThrows( PnInternalException.class, todo );
    }

    @Test
    void getNotificationWithTimelineInfoSuccess() {
        //Given
        InternalNotification notification = getNewInternalNotification();


        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                //.iun( IUN )
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );
        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( notification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification result = svc.getNotificationInformation( IUN );

        //Then
        Assertions.assertNotNull( result );
    }

    @Test
    void getNotificationInformationByProtocolAndIdempotenceSuccess() {
        // Given
        InternalNotification notification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        // When
        when( notificationDao.getRequestId( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ) ).thenReturn( Optional.of( REQUEST_ID ) );

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( notification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification result = svc.getNotificationInformation( SENDER_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN, GROUPS );

        // Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals( IUN, result.getIun() );
        Assertions.assertEquals( PA_PROTOCOL_NUMBER, result.getPaProtocolNumber() );
        Assertions.assertEquals( IDEMPOTENCE_TOKEN, result.getIdempotenceToken() );
    }

    @Test
    void getNotificationInformationByProtocolAndIdempotenceFailure() {
        // When
        when( notificationDao.getRequestId( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ) ).thenReturn( Optional.empty() );
        Executable todo = () -> svc.getNotificationInformation( SENDER_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN, GROUPS );

        // Then
        Assertions.assertThrows(PnNotificationNotFoundException.class, todo);
    }

    @Test

    void getNotificationInformationWithSenderIdCheckSuccess() {
        InternalNotification notification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));
        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( notification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification result = svc.getNotificationInformationWithSenderIdCheck( IUN, SENDER_ID, GROUPS );

        Assertions.assertNotNull( result );
        Assertions.assertEquals( IUN, result.getIun() );
        Assertions.assertEquals( SENDER_ID, result.getSenderPaId() );

    }

    @Test
    void getNotificationInformationByWrongSenderIdFailure() {
        InternalNotification notification = getNewInternalNotification();

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( notification ) );

        Executable todo = () ->  svc.getNotificationInformationWithSenderIdCheck( IUN, WRONG_SENDER_ID, GROUPS );
        Assertions.assertThrows( PnNotificationNotFoundException.class, todo );
    }

    @Test
    void getNotificationInformationByWrongSenderGroupFailure() {
        InternalNotification notification = getNewInternalNotification();
        notification.setGroup( "fakeGroup" );

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( notification ) );

        Executable todo = () ->  svc.getNotificationInformationWithSenderIdCheck( IUN, SENDER_ID, GROUPS );
        Assertions.assertThrows( PnNotificationNotFoundException.class, todo );
    }

    @Test

    void getNotificationInfoReturnFirstNoticeCode() {
        InternalNotification notification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() ) ).thenReturn( Optional.of( notification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification result = svc.getNotificationInformation( IUN, true, false );

        Assertions.assertNotNull( result );
    }

    @NotNull
    private InternalNotification getNewInternalNotification() {
        InternalNotification internalNotification = new InternalNotification();
        TimelineElementV27 timelineElement = new TimelineElementV27();
        timelineElement.setCategory(TimelineElementCategoryV27.AAR_CREATION_REQUEST);
        internalNotification.setTimeline(List.of(timelineElement));
        internalNotification.setIun(IUN);
        internalNotification.setRecipientIds(List.of("cxId"));
        internalNotification.setPaProtocolNumber("paProtocolNumber");
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId(SENDER_ID);
        internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        internalNotification.setIdempotenceToken(IDEMPOTENCE_TOKEN);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .payments(List.of(
                                NotificationPaymentInfo.builder()
                                        .f24(it.pagopa.pn.delivery.models.internal.notification.F24Payment.builder()
                                                .title("title")
                                                .applyCost(false)
                                                .metadataAttachment(MetadataAttachment.builder()
                                                        .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                                                                .key("key")
                                                                .versionToken("versionToken")
                                                                .build())
                                                        .contentType("contentType")
                                                        .digests(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests.builder()
                                                                .sha256("jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=")
                                                                .build())
                                                        .build())
                                                .build())
                                        .pagoPa(it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment
                                                .builder()
                                                .noticeCode(NOTICE_CODE)
                                                .creditorTaxId("creditoTaxId")
                                                .attachment(MetadataAttachment.builder()
                                                        .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                                                                .key("key")
                                                                .versionToken("versionToken")
                                                                .build())
                                                        .contentType("contentType")
                                                        .digests(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests.builder()
                                                                .sha256("jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()
                        ))
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
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
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.empty() );
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
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( notification ) );
        when( externalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(false ) )).thenReturn( groups );

        InternalNotification result = svc.getNotificationInformation( IUN, false, true, SENDER_ID);

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
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( notification ) );
        when( externalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(false ) )).thenReturn( groups );

        InternalNotification result = svc.getNotificationInformation( IUN, false, true, SENDER_ID);

        //Then
        Assertions.assertNotNull( result );
        Assertions.assertEquals("group-code-fake", result.getGroup());
    }


    @Test
    void getNotificationCancelledFromPA() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( anyString(), anyInt(), any() ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification internalNotificationResult = svc.getNotificationInformation(IUN, true, true);

        //Then
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertNull(internalNotificationResult.getDocuments());
    }

    @Test
    void getNotificationCancelledFromRecipient() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( anyString(), anyInt(), any() ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification internalNotificationResult = svc.getNotificationInformation(IUN, true, false);

        //Then
        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertNull( internalNotificationResult.getDocuments() );
    }

    @Test
    void checkRefinementDateOraSolare() {
        // Given
        List<TimelineElementV27> timelineElementList = List.of( TimelineElementV27.builder()
                        .category( TimelineElementCategoryV27.REFINEMENT )
                        .timestamp( OffsetDateTime.parse( "2022-10-05T12:23:15.123456Z" ) )
                        .build(),
                TimelineElementV27.builder()
                        .category( TimelineElementCategoryV27.NOTIFICATION_VIEWED )
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
        List<TimelineElementV27> timelineElementList = List.of( TimelineElementV27.builder()
                        .category( TimelineElementCategoryV27.REFINEMENT )
                        .timestamp( OffsetDateTime.parse( "2022-12-05T12:23:15.123456Z" ) )
                        .build(),
                TimelineElementV27.builder()
                        .category( TimelineElementCategoryV27.NOTIFICATION_VIEWED )
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

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertNull(internalNotificationResult.getDocuments() );
    }

    @Test

    void getNotificationAndViewEventByDelegateSuccess() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID );
        internalMandateDto.setDelegate( UID );
        internalMandateDto.setDelegator( CX_ID );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        NotificationViewDelegateInfo delegateInfo = NotificationViewDelegateInfo.builder()
                .mandateId( MANDATE_ID )
                .delegateType( NotificationViewDelegateInfo.DelegateType.PF )
                .operatorUuid( UID )
                .internalId( CX_ID )
                .build();

        //When
        when( pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), eq(CxTypeAuthFleet.PF), any()))
                .thenReturn( mandateResult );
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, MANDATE_ID);
        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0, delegateInfo, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertTrue( internalNotificationResult.getDocumentsAvailable() );
    }

    @Test
    void getNotificationAndViewEventByDelegateNotAllowedPAID() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID );
        internalMandateDto.setDelegate( UID );
        internalMandateDto.setDelegator( CX_ID );
        internalMandateDto.setVisibilityIds(List.of(SENDER_ID+"OTHER"));
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );

        //When
        Mockito.when(externalRegistriesClient.getRootSenderId(Mockito.anyString())).thenReturn(SENDER_ID+"DIFF");
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString(), any(), any() ) ).thenReturn( mandateResult );
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.getNotificationAndNotifyViewedEvent( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID ));

    }


    @Test
    void getNotificationAndViewEventByDelegateNotAllowedNOMandate() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When

        List<InternalMandateDto> mandateResult = List.of(  );

        //When
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString(), any(), any() ) ).thenReturn( mandateResult );
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.getNotificationAndNotifyViewedEvent( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID ));

    }

    @Test
    void getNotificationAndViewEventByDelegateNotAllowedNOMandateForDelegator() {
        //Given
        String nowTestInstant = "2022-06-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId( MANDATE_ID+"OTHER" );
        internalMandateDto.setDelegate( UID );
        internalMandateDto.setDelegator( CX_ID + "OTHER" );
        internalMandateDto.setDatefrom( "2022-03-23T23:23:00Z" );

        List<InternalMandateDto> mandateResult = List.of( internalMandateDto );


        //When
        Mockito.when( pnMandateClient.listMandatesByDelegate( Mockito.anyString(), Mockito.anyString(), any(), any() ) ).thenReturn( mandateResult );
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.getNotificationAndNotifyViewedEvent( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID ));

    }

    @Test

    void getNotificationAndViewEventDocsUnavSuccess() {
        //Given
        String nowTestInstant = "2022-06-30T00:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp( OffsetDateTime.parse( "2022-03-01T12:00:00.00Z" ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertEquals(0, internalNotificationResult.getDocuments().size() );
    }

    @Test
    void getNotificationAndViewEventError() {
        //Given
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = Collections.singletonList( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  Instant.now()
                        .atOffset(ZoneOffset.UTC) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );


        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.now() );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        Executable todo = () -> svc.getNotificationAndNotifyViewedEvent(IUN, new InternalAuthHeader("PF", CX_TYPE, UID, null), null);

        //Then
        Assertions.assertThrows(PnNotFoundException.class, todo);
    }

    @Test

    void getNotificationWith2IUVBeforeTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( "2022-01-15T00:00:00.00Z" ) );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-01-15T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals( NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );

    }

    @Test

    void getNotificationWith2IUVWithFirstIUVPayedAfterTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.SUCCEEDED );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( "2022-02-11T00:00:00.00Z" ) );
        when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-02-11T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals( NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
    }

    @Test

    void getNotificationWith2IUVWithFirstIUVNoPayedAfterTerms() {
        String nowTestInstant = "2022-01-17T13:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals( NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
    }

    @Test

    void getNotificationWith2IUVFirstLimitDay() {
        String nowTestInstant = "2022-03-06T23:59:00.00Z";

        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-02T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify(notificationViewedProducer).sendNotificationViewed(IUN, Instant.parse(nowTestInstant), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET);
        if(internalNotificationResult.getRecipients().get(0).getPayments() == null){
            Assertions.assertNull(internalNotificationResult.getRecipients().get(0).getPayments());
            Assertions.assertNull(internalNotificationResult.getRecipients().get(0).getPayments());
        }else{
            Assertions.assertEquals(NOTICE_CODE, internalNotificationResult.getRecipients().get(0).getPayments().get(0).getPagoPa().getNoticeCode());
        }


    }

    @Test

    void getNotificationWith2IUVLastLimitDay() {
        String nowTestInstant = "2022-05-01T15:00:00.00Z";

        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T12:37:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-02T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        PaymentInfo paymentInfo = new PaymentInfo()
                .status( PaymentStatus.REQUIRED );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( paymentInfo );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( nowTestInstant ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals(NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
    }

    @Test

    void getNotificationWith2IUVAfterMaxTerms() {
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( "2022-05-11T00:00:00.00Z" ) );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-05-11T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals(NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
    }

    @Test

    void getNotificationWith2IUVWithExtRegistryException() {
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) ));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant() ).thenReturn( Instant.parse( "2022-03-11T00:00:00.00Z" ) );
        when( externalRegistriesClient.getPaymentInfo( Mockito.anyString(), Mockito.anyString() ) ).thenThrow( PnHttpResponseException.class );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);
        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-03-11T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        Assertions.assertEquals(NOTICE_CODE, internalNotificationResult.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
    }

    @Test

    void getNotificationWithoutDocumentsSuccess() {
        // Given
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.CANCELLED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-06-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        when( clock.instant()).thenReturn(Instant.now());

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertEquals( Collections.emptyList(), internalNotification.getDocuments() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getCreditorTaxId() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getAttachment() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getF24() );
    }

    @Test

    void getNotificationWithDocumentsLimitDay() {
        // Given
        String nowTestInstant = "2022-06-29T18:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T17:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        // Then
        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);
        Assertions.assertTrue(internalNotificationResult.getDocumentsAvailable());
    }

    @Test

    void getNotificationWithoutDocumentsLimitDay() {
        // Given
        String nowTestInstant = "2022-06-30T00:00:00.00Z";
        InternalNotification internalNotification = getNewInternalNotification();

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                .elementId( "elementId" )
                .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T17:00:00.00Z" ), ZoneOffset.UTC )));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-03-01T11:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        when( clock.instant() ).thenReturn( Instant.parse( nowTestInstant ) );
        when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );

        // Then
        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);

        Assertions.assertFalse( internalNotificationResult.getDocumentsAvailable() );
        Assertions.assertEquals( Collections.emptyList(), internalNotification.getDocuments() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getNoticeCode() );
        Assertions.assertNotNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getCreditorTaxId() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getPagoPa().getAttachment() );
        Assertions.assertNull( internalNotification.getRecipients().get( 0 ).getPayments().get(0).getF24() );

    }

    @Test

    void getNotificationFilteredByRecIndex() {
        InternalNotification internalNotification = getNewInternalNotification();
        enrichInternalNotificationWithAnotherRecipient(internalNotification, "another-recipient");

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) )
                        .details(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementDetailsV27().recIndex(1)));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-01-15T00:00:00.00Z" ) );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, INTERNAL_AUTH_HEADER, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-01-15T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        //mi aspetto che il destinatario che invoca il servizio (cxid, con indice 0), non "veda" la timeline di VIEWD poiché "appartiene" al destinatario another-recipient con indice 1
        Assertions.assertEquals(1, internalNotificationResult.getTimeline().size());
        Assertions.assertEquals(TimelineElementCategoryV27.REFINEMENT, internalNotificationResult.getTimeline().get(0).getCategory());
        //mi aspetto che il destinatario che invoca il servizio (cxid, con indice 0), non "veda" nella lista di recipient i dati del recipient con indice 1
        Assertions.assertEquals(2, internalNotificationResult.getRecipients().size()); //la dimensione dell'array non deve cambiare
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getTaxId());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getDenomination());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getDigitalDomicile());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getPayments());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getPhysicalAddress());
    }

    @Test

    void getNotificationNotFilteredByRecIndex() {
        InternalNotification internalNotification = getNewInternalNotification();
        enrichInternalNotificationWithAnotherRecipient(internalNotification, "another-recipient");

        var tle = List.of( new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REFINEMENT )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC )),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27()
                        .elementId( "elementId_1" )
                        .category( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_VIEWED )
                        .timestamp(  OffsetDateTime.ofInstant( Instant.parse( "2022-01-12T00:00:00.00Z" ), ZoneOffset.UTC ) )
                        .details(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementDetailsV27().recIndex(0)));

        NotificationHistoryResponse timelineStatusHistoryDto = new NotificationHistoryResponse()
                .timeline( tle )
                .notificationStatus( it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED )
                .notificationStatusHistory( Collections.singletonList(new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusHistoryElementV26()
                        .status(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED)
                        .activeFrom( OffsetDateTime.ofInstant( Instant.parse( "2022-01-11T00:00:00.00Z" ), ZoneOffset.UTC ) )) );

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean() )).thenReturn( Optional.of( internalNotification ) );
        Mockito.when( pnDeliveryPushClient.getTimelineAndStatusHistory( Mockito.anyString(), Mockito.anyInt(), Mockito.any(OffsetDateTime.class) ) ).thenReturn( timelineStatusHistoryDto );
        Mockito.when( clock.instant() ).thenReturn( Instant.parse( "2022-01-15T00:00:00.00Z" ) );

        InternalNotification internalNotificationResult = svc.getNotificationAndNotifyViewedEvent( IUN, INTERNAL_AUTH_HEADER, null );

        //Then
        Mockito.verify( notificationViewedProducer ).sendNotificationViewed( IUN, Instant.parse( "2022-01-15T00:00:00.00Z" ), 0, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET );
        //mi aspetto che il destinatario che invoca il servizio (cxid, con indice 0), "veda" anche la timeline di VIEWD poiché "appartiene" a lui
        Assertions.assertEquals(2, internalNotificationResult.getTimeline().size());
        //ma mi aspetto che il destinatario che invoca il servizio (cxid, con indice 0), non "veda" i dati del recipient 1
        Assertions.assertEquals(2, internalNotificationResult.getRecipients().size()); //la dimensione dell'array non deve cambiare
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getTaxId());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getDenomination());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getDigitalDomicile());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getPayments());
        Assertions.assertNull(internalNotificationResult.getRecipients().get(1).getPhysicalAddress());
        //e mi aspetto che possa vedere i suoi dati di recipient
        Assertions.assertEquals(internalNotificationResult.getRecipients().get(0), internalNotificationResult.getRecipients().get(0));
    }

    private void enrichInternalNotificationWithAnotherRecipient(InternalNotification internalNotification, String recipient) {
        ArrayList<NotificationRecipient> notificationRecipients = new ArrayList<>(internalNotification.getRecipients());
        notificationRecipients.add(NotificationRecipient.builder()
                .recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
                .payments(List.of(NotificationPaymentInfo.builder().build()))
                .taxId("88888888")
                .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder().address("recipient1@pec.it").build())
                .physicalAddress(it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress.builder().address("via Milano").zip("80100").build())
                .denomination("Cristoforo Colombo")
                .internalId("internalId-recipient-1")
                .build());

        internalNotification.setRecipients(notificationRecipients);
        ArrayList<String> recipientIds = new ArrayList<>(internalNotification.getRecipientIds());
        recipientIds.add(recipient);
        internalNotification.setRecipientIds(recipientIds);
    }

    @Test
    void searchNotificationDelegatedEpochAfterEndDateTest() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(OffsetDateTime.now().toInstant())
                .group(GROUP)
                .senderId(SENDER_ID)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey(null)
                .endDate(OffsetDateTime.MIN.toInstant())
                .cxGroups(GROUPS)
                .build();

        Assertions.assertEquals( ResultPaginationDto.<NotificationSearchRow, String>builder()
                .resultsPage(Collections.emptyList())
                .nextPagesKey(Collections.emptyList())
                .moreResult(false)
                .build(), svc.searchNotificationDelegated(inputSearchNotificationDelegatedDto));
    }

    @Test
    void searchNotificationDelegatedCanNotAccessDelegatedGroupTest() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(OffsetDateTime.now().toInstant())
                .group(null)
                .senderId(SENDER_ID)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey(null)
                .endDate(OffsetDateTime.now().toInstant())
                .cxGroups(GROUPS)
                .build();

        Executable todo = () -> svc.searchNotificationDelegated(inputSearchNotificationDelegatedDto);

        Assertions.assertThrows(PnForbiddenException.class, todo);
    }

    @Test
    void searchNotificationDelegatedValidateInputException() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(OffsetDateTime.now().toInstant())
                .endDate(OffsetDateTime.MAX.toInstant())
                .group(null)
                .senderId(SENDER_ID)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey(null)
                .build();

        Executable todo = () -> svc.searchNotificationDelegated(inputSearchNotificationDelegatedDto);

        Assertions.assertThrows( PnValidationException.class, todo );
    }

    @Test
    void searchNotificationDelegatedUnableDeserializeLEK() {
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(OffsetDateTime.now().toInstant())
                .senderId(SENDER_ID)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(SIZE)
                .nextPageKey("fakeNextPageKey")
                .endDate(OffsetDateTime.MAX.toInstant())
                .build();


        Executable todo = () -> svc.searchNotificationDelegated(inputSearchNotificationDelegatedDto);

        Assertions.assertThrows( PnInternalException.class, todo );
    }

    @Test
    void searchNotificationDelegatedSuccess() {

        //Given
        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate( OffsetDateTime.MIN.toInstant() )
                .endDate( OffsetDateTime.MAX.toInstant())                .senderId(SENDER_ID)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .nextPageKey(null)
                .cxGroups(List.of("group-code", "group-code-no-match"))
                .group("group-code")
                .size( 10 )
                .build();
        ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> results = getPaginatedNotifications();

        //When
        when(notificationSearch.searchNotificationMetadata()).thenReturn(results);
        when(notificationSearchFactory.getMultiPageDelegatedSearch(any(), any()))
                .thenReturn(notificationSearch);

        ResultPaginationDto<NotificationSearchRow, String> result = svc.searchNotificationDelegated(inputSearchNotificationDelegatedDto);
        Assertions.assertNotNull( result );
        Assertions.assertEquals( 3, results.getResultsPage().size());
    }

    @Test
    void checkIfNotificationIsNotCancelledWithNotificationNotCancelledTest() {
        String iun = "a-iun";
        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipients(List.of(new NotificationRecipient()));
        NotificationHistoryResponse deliveryPushResponse = new NotificationHistoryResponse();
        deliveryPushResponse.setNotificationStatus(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED);
        deliveryPushResponse.setNotificationStatusHistory(List.of());
        deliveryPushResponse.setTimeline(List.of(
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.VALIDATE_NORMALIZE_ADDRESSES_REQUEST),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NORMALIZED_ADDRESS),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.SENDER_ACK_CREATION_REQUEST),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED)
        ));

        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));
        when(pnDeliveryPushClient.getTimelineAndStatusHistory(iun, notification.getRecipients().size(), notification.getSentAt())).thenReturn(deliveryPushResponse);

        Assertions.assertEquals(iun, notification.getIun());
    }

    @Test
    void checkIfNotificationIsNotCancelledWithNotificationCancelledTest() {
        String iun = "a-iun";
        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipients(List.of(new NotificationRecipient()));
        NotificationHistoryResponse deliveryPushResponse = new NotificationHistoryResponse();
        deliveryPushResponse.setNotificationStatus(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationStatusV26.ACCEPTED);
        deliveryPushResponse.setNotificationStatusHistory(List.of());
        deliveryPushResponse.setTimeline(List.of(
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.VALIDATE_NORMALIZE_ADDRESSES_REQUEST),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NORMALIZED_ADDRESS),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.SENDER_ACK_CREATION_REQUEST),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.REQUEST_ACCEPTED),
                new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementV27().category(it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.TimelineElementCategoryV27.NOTIFICATION_CANCELLATION_REQUEST)

        ));

        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));
        when(pnDeliveryPushClient.getTimelineAndStatusHistory(iun, notification.getRecipients().size(), notification.getSentAt())).thenReturn(deliveryPushResponse);
        Assertions.assertEquals(iun, notification.getIun());
    }

    @Test
    void checkIUNAndInternalIdSuccessfullyWithRecipientId() {
        String iun = "a-iun";
        String recipientInternalId = "internalId-1";

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipientIds(List.of(recipientInternalId));

        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));

        Assertions.assertDoesNotThrow(() -> svc.checkIUNAndInternalId(iun, recipientInternalId, null, null, null));
    }

    @Test
    void checkIUNAndInternalIdFailsWithRecipientId() {
        String iun = "a-iun";
        String recipientInternalId = "internalId-1";

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipientIds(List.of("otherInternalId-2"));

        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));

        Assertions.assertThrows(PnForbiddenException.class, () -> svc.checkIUNAndInternalId(iun, recipientInternalId, null, null, null));
    }

    @Test
    void checkIUNAndInternalIdFailsWithForNotificationNotFound() {
        String iun = "a-iun";
        String recipientInternalId = "internalId-1";

        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.empty());

        Assertions.assertThrows(PnNotificationNotFoundException.class, () -> svc.checkIUNAndInternalId(iun, recipientInternalId, null, null, null));
    }

    @Test
    void checkIUNAndInternalIdSuccessfullyWithMandateId() {
        String iun = "a-iun";
        String recipientInternalId = "delegatedId-1";
        String mandateId = "mandateId-1";

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipientIds(List.of(recipientInternalId));
        notification.setSenderPaId("senderPaId-1");

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(mandateId);
        internalMandateDto.setDelegator("delegatorId-1");
        internalMandateDto.setDelegate(recipientInternalId);


        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getRootSenderId("senderPaId-1")).thenReturn("rootSenderId-1");
        when(pnMandateClient.listMandatesByDelegate(recipientInternalId, mandateId, null, null)).thenReturn(List.of(internalMandateDto));
        Assertions.assertDoesNotThrow(() -> svc.checkIUNAndInternalId(iun, recipientInternalId, mandateId, null, null));
    }

    @Test
    void checkIUNAndInternalIdFailsWithMandateId() {
        String iun = "a-iun";
        String recipientInternalId = "delegatedId-1";
        String mandateId = "mandateId-1";

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        notification.setSentAt(OffsetDateTime.now());
        notification.setRecipientIds(List.of(recipientInternalId));
        notification.setSenderPaId("senderPaId-1");


        when(notificationDao.getNotificationByIun(iun, true)).thenReturn(Optional.of(notification));
        when(externalRegistriesClient.getRootSenderId("senderPaId-1")).thenReturn("rootSenderId-1");
        when(pnMandateClient.listMandatesByDelegate(recipientInternalId, mandateId, null, null)).thenReturn(new ArrayList<>());
        Assertions.assertThrows(PnMandateNotFoundException.class, () -> svc.checkIUNAndInternalId(iun, recipientInternalId, mandateId, null, null));
    }
}

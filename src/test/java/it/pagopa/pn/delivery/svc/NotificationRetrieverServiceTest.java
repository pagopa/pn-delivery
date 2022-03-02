package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntryId;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClient;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRetrieverServiceTest {
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String KEY = "KEY";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    public static final NotificationAttachment NOTIFICATION_INLINE_ATTACHMENT = NotificationAttachment.builder()
            .body(BASE64_BODY)
            .contentType("Content/Type")
            .digests(NotificationAttachment.Digests.builder()
                    .sha256(SHA256_BODY)
                    .build()
            )
            .ref( NotificationAttachment.Ref.builder()
                    .key( KEY )
                    .versionToken( VERSION_TOKEN )
                    .build() )
            .build();

    public static final NotificationAttachment NOTIFICATION_REFERRED_ATTACHMENT = NotificationAttachment.builder()
            .ref( NotificationAttachment.Ref.builder()
                    .versionToken( VERSION_TOKEN )
                    .key( KEY )
                    .build() )
            .contentType("Content/Type")
            .build();

    private static final boolean BY_SENDER = true;
    private static final String PA_NOTIFICATION_ID = "PA_NOTIFICATION_ID";
    private static final String SUBJECT = "SUBJECT";
    private static final Instant TIMESTAMP = Instant.parse( "2021-12-10T00:00:00Z" );
    private static final String IUN = "IUN";
    private static final String USER_ID = "USER_ID";
    private static final String SENDER_ID = "SENDER_ID";
    private static final Instant START_DATE = Instant.parse( "2021-12-09T00:00:00Z" );
    private static final Instant END_DATE = Instant.parse( "2021-12-10T00:00:00Z" );
    private static final String DOWNLOAD_URL = "http://fake-download-url";

    private S3PresignedUrlService s3PresignedUrlService;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private PnDeliveryPushClient pnDeliveryPushClient;
    private StatusUtils statusUtils;
    private FileStorage fileStorage;
    private NotificationRetrieverService notificationRetrieverService;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

        s3PresignedUrlService = Mockito.mock( S3PresignedUrlService.class );
        notificationViewedProducer = Mockito.mock( NotificationViewedProducer.class );
        notificationDao = Mockito.mock( NotificationDao.class );
        pnDeliveryPushClient = Mockito.mock( PnDeliveryPushClient.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        fileStorage = Mockito.mock( FileStorage.class );

        notificationRetrieverService = new NotificationRetrieverService(
                clock,
                fileStorage,
                s3PresignedUrlService,
                notificationViewedProducer,
                notificationDao,
                pnDeliveryPushClient,
                statusUtils);
    }

    
    @Test
    void searchNotificationSuccess() {
        
        //Given
        List<NotificationSearchRow> notificationSearchRowList = new ArrayList<>();
        notificationSearchRowList.add( NotificationSearchRow.builder()
                        .paNotificationId( PA_NOTIFICATION_ID )
                        .subject( SUBJECT )
                        .sentAt( TIMESTAMP )
                        .notificationStatus( NotificationStatus.VIEWED )
                        .iun( IUN )
                        .recipientId(USER_ID)
                        .senderId( SENDER_ID )
                        .build());

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( notificationSearchRowList );

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(10)
                .nextPagesKey(null)
                .build();
        
        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList, result.getResult() );
    }

    @Test
    void searchNotificationOneResultPagination() {

        //Given
        List<NotificationSearchRow> notificationSearchRowList = new ArrayList<>();
        notificationSearchRowList.add( NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( TIMESTAMP )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( notificationSearchRowList );

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(10)
                .nextPagesKey(null)
                .build();

        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList, result.getResult() );
        assertFalse(result.isMoreResult());
        assertNull(result.getNextPagesKey());
    }

    @Test
    void searchNotificationPaginationNoResult() {
        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( null );

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(10)
                .nextPagesKey(null)
                .build();

        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertNull(result.getResult() );
        assertFalse(result.isMoreResult());
        assertNull(result.getNextPagesKey());

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( Collections.emptyList() );

        result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(Collections.emptyList(), result.getResult() );
        assertFalse(result.isMoreResult());
        assertNull(result.getNextPagesKey());
    }

    @Test
    void searchNotificationPaginationSizeEqualsResult() {

        //Given
        List<NotificationSearchRow> notificationSearchRowList = new ArrayList<>();
        notificationSearchRowList.add( NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( TIMESTAMP )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());
        notificationSearchRowList.add( NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( TIMESTAMP )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( notificationSearchRowList );

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(2)
                .nextPagesKey(null)
                .build();

        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList, result.getResult() );
        assertFalse(result.isMoreResult());
        assertNull(result.getNextPagesKey());
    }

    @Test
    void searchNotificationPaginationResultBiggerThenSize() {
        Instant dateFirstElementNextPage = Instant.now();
        
        //Given
        List<NotificationSearchRow> notificationSearchRowList = new ArrayList<>();
        NotificationSearchRow notSearchRow = NotificationSearchRow.builder()
                .paNotificationId(PA_NOTIFICATION_ID)
                .subject(SUBJECT)
                .sentAt(TIMESTAMP)
                .notificationStatus(NotificationStatus.VIEWED)
                .iun(IUN)
                .recipientId(USER_ID)
                .senderId(SENDER_ID)
                .build();
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add( NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( dateFirstElementNextPage )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());
        notificationSearchRowList.add(notSearchRow);

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( notificationSearchRowList);

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(2)
                .nextPagesKey(null)
                .build();

        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList.subList(0, searchDto.getSize()), result.getResult() );
        assertFalse(result.isMoreResult());
        assertEquals(dateFirstElementNextPage.toString(), result.getNextPagesKey().get(0));
    }

    @Test
    void searchNotificationPaginationResultBiggerThenSizeWithMoreResult() {
        Instant dateFirstElementNextPage = Instant.now();
        Instant dateSecondElementNextPage = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant dateThirdElementNextPage = Instant.now().plus(3, ChronoUnit.DAYS);

        //Given
        List<NotificationSearchRow> notificationSearchRowList = new ArrayList<>();
        NotificationSearchRow notSearchRow = NotificationSearchRow.builder()
                .paNotificationId(PA_NOTIFICATION_ID)
                .subject(SUBJECT)
                .sentAt(TIMESTAMP)
                .notificationStatus(NotificationStatus.VIEWED)
                .iun(IUN)
                .recipientId(USER_ID)
                .senderId(SENDER_ID)
                .build();
        
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add( NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( dateFirstElementNextPage )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add(NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( dateSecondElementNextPage )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add(NotificationSearchRow.builder()
                .paNotificationId( PA_NOTIFICATION_ID )
                .subject( SUBJECT )
                .sentAt( dateThirdElementNextPage )
                .notificationStatus( NotificationStatus.VIEWED )
                .iun( IUN )
                .recipientId(USER_ID)
                .senderId( SENDER_ID )
                .build());
        notificationSearchRowList.add(notSearchRow);
        notificationSearchRowList.add(notSearchRow);

        //Where
        Mockito.when( notificationDao.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn( notificationSearchRowList);

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(BY_SENDER)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(USER_ID)
                .status(NotificationStatus.VIEWED)
                .subjectRegExp(SUBJECT)
                .size(2)
                .nextPagesKey(null) 
                .build();

        ResultPaginationDto<NotificationSearchRow> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList.subList(0, searchDto.getSize()), result.getResult() );
        assertTrue(result.isMoreResult());
        assertEquals(dateFirstElementNextPage.toString(), result.getNextPagesKey().get(0));
        assertEquals(dateSecondElementNextPage.toString(), result.getNextPagesKey().get(1));
        assertEquals(dateThirdElementNextPage.toString(), result.getNextPagesKey().get(2));
    }
    
    @Test
    void getNotificationInformationSuccess() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( pnDeliveryPushClient.getTimelineElements( Mockito.anyString()) ).thenReturn( Collections.emptySet() );
        Mockito.when( statusUtils.getStatusHistory( Mockito.anySet(), Mockito.anyInt(), Mockito.any( Instant.class ) ) )
                .thenReturn( Collections.emptyList() );
        Notification result = notificationRetrieverService.getNotificationInformation( IUN );

        //Then
        assertEquals( result, notification );
    }

    @Test
    void getNotificationInformationSuccessWithTimeline() {
        //Given
        Notification notification = newNotificationWithTimeline();
        Set<TimelineElement> timelineElements = new HashSet<>();
        timelineElements.add( notification.getTimeline().get(0));

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( pnDeliveryPushClient.getTimelineElements( Mockito.anyString()) ).thenReturn( timelineElements );
        Mockito.when( statusUtils.getStatusHistory( Mockito.anySet(), Mockito.anyInt(), Mockito.any( Instant.class ) ) )
                .thenReturn( Collections.emptyList() );
        Notification result = notificationRetrieverService.getNotificationInformation( IUN );

        //Then
        assertEquals( result, notification );
    }


    @Test
    void getNotificationInformationFailure() {
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.empty() );
        Executable todo = () -> notificationRetrieverService.getNotificationInformation( IUN );
        //Then
        assertThrows(PnInternalException.class, todo);

    }

    @Test
    void getNotificationAndNotifySuccess() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Notification resultNotification = notificationRetrieverService.getNotificationAndNotifyViewedEvent( IUN, USER_ID );

        //Then
        assertEquals( notification, resultNotification );
    }

    @Test
    void getNotificationAndNotifyFailure() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Executable todo = () -> notificationRetrieverService.getNotificationAndNotifyViewedEvent( IUN, "");

        //Then
        assertThrows( PnInternalException.class, todo );
    }

    @Test
    void getNotificationAndNotifyRecipientNotFoundFailure() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Executable todo = () -> notificationRetrieverService.getNotificationAndNotifyViewedEvent( IUN, "DIFFERENT_USER");

        //Then
        assertThrows( PnInternalException.class, todo );
    }

    @Test
    void downloadDocumentFailure() {
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.empty() );
        Executable todo = () -> notificationRetrieverService.downloadDocument( IUN, 0 );

        //Then
        assertThrows( PnInternalException.class, todo );
    }

    @Test
    void downloadDocumentWithRedirectSuccess() {
        //Given
        Notification notification = newNotificationWithDocs();
        PreloadResponse preloadResponse = PreloadResponse.builder()
                .url( DOWNLOAD_URL )
                .build();
        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( s3PresignedUrlService.presignedDownload( Mockito.anyString(),
                Mockito.any( NotificationAttachment.class ) ) )
                .thenReturn( preloadResponse );
        String responseUrl = notificationRetrieverService.downloadDocumentWithRedirect( IUN, 0 );

        //Then
        assertEquals( preloadResponse.getUrl(), responseUrl );
    }

    @Test
    void downloadDocumentWithRedirectFailure() {
        //When
        Executable todo = () -> notificationRetrieverService.downloadDocumentWithRedirect( IUN, 0 );

        //Then
        assertThrows( PnInternalException.class, todo );
    }

    private Notification newNotificationWithTimeline( ) {
        return Notification.builder()
                .iun(IUN)
                .paNotificationId(PA_NOTIFICATION_ID)
                .subject(SUBJECT)
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(SENDER_ID)
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId(USER_ID)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()))
                .timeline( Collections.singletonList(TimelineElement.builder()
                        .iun(IUN)
                        .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                        .timestamp(Instant.now())
                                        .details( SendPaperFeedbackDetails.sendPaperFeedbackBuilder()
                                                .newAddress( PhysicalAddress.builder().build() )
                                                .build()  )
                                        .legalFactsIds( Collections.singletonList( LegalFactsListEntryId.builder()
                                                        .type( LegalFactType.ANALOG_DELIVERY )
                                                        .key( KEY )
                                                        .build()))
                        .build()))
                .notificationStatusHistory( Collections.emptyList() )
                .build();
    }

    private Notification newNotificationWithoutPayments( ) {
        return Notification.builder()
                .iun(IUN)
                .paNotificationId(PA_NOTIFICATION_ID)
                .subject(SUBJECT)
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(SENDER_ID)
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId(USER_ID)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()))
                .timeline( Collections.emptyList() )
                .notificationStatusHistory( Collections.emptyList() )
                .build();
    }

    private Notification newNotificationWithDocs( ) {
        return Notification.builder()
                .iun(IUN)
                .paNotificationId(PA_NOTIFICATION_ID)
                .subject(SUBJECT)
                .physicalCommunicationType(ServiceLevelType.SIMPLE_REGISTERED_LETTER)
                .sender(NotificationSender.builder()
                        .paId(SENDER_ID)
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId(USER_ID)
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()))
                .timeline( Collections.emptyList() )
                .notificationStatusHistory( Collections.emptyList() )
                .documents(Arrays.asList(
                        NOTIFICATION_INLINE_ATTACHMENT,
                        NOTIFICATION_INLINE_ATTACHMENT
                ))
                .build();
    }

    private Notification newNotificationWithoutPaymentsWithRef( ) {
        return  newNotificationWithoutPayments().toBuilder()
                .documents(Arrays.asList(
                        NOTIFICATION_REFERRED_ATTACHMENT
                ))
                .build();
    }
  
}

package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperFeedbackDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.pnclient.externalchannel.ExternalChannelClient;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String CONTENT_TYPE = "Content-Type";
    private static final long CONTENT_LENGTH = 0L;
    private static final String EXT_CHA_LEGAL_FACT_ID = "extcha_LEGAL_FACT_ID";
    private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";

    private AttachmentService attachmentService;
    private S3PresignedUrlService s3PresignedUrlService;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private TimelineDao timelineDao;
    private StatusUtils statusUtils;
    private FileStorage fileStorage;
    private ExternalChannelClient externalChannelClient;
    private LegalfactsMetadataUtils legalFactMetadata;
    private NotificationRetrieverService notificationRetrieverService;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

        s3PresignedUrlService = Mockito.mock( S3PresignedUrlService.class );
        notificationViewedProducer = Mockito.mock( NotificationViewedProducer.class );
        notificationDao = Mockito.mock( NotificationDao.class );
        timelineDao = Mockito.mock( TimelineDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );
        fileStorage = Mockito.mock( FileStorage.class );
        externalChannelClient = Mockito.mock( ExternalChannelClient.class );
        legalFactMetadata = Mockito.mock(LegalfactsMetadataUtils.class);

        attachmentService = new AttachmentService( fileStorage,
                legalFactMetadata,
                Mockito.mock( NotificationReceiverValidator.class ),
                timelineDao,
                externalChannelClient);

        notificationRetrieverService = new NotificationRetrieverService(
                clock,
                attachmentService,
                s3PresignedUrlService,
                notificationViewedProducer,
                notificationDao,
                timelineDao,
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
        
        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
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

        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
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

        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
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

        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
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

        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList.subList(0, searchDto.getSize()), result.getResult() );
        assertFalse(result.isMoreResult());
        assertEquals(dateFirstElementNextPage, result.getNextPagesKey().get(0));
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

        ResultPaginationDto<NotificationSearchRow, Instant> result = notificationRetrieverService.searchNotification(searchDto);
        //Then
        assertEquals(notificationSearchRowList.subList(0, searchDto.getSize()), result.getResult() );
        assertTrue(result.isMoreResult());
        assertEquals(dateFirstElementNextPage, result.getNextPagesKey().get(0));
        assertEquals(dateSecondElementNextPage, result.getNextPagesKey().get(1));
        assertEquals(dateThirdElementNextPage, result.getNextPagesKey().get(2));
    }
    
    @Test
    void getNotificationInformationSuccess() {
        //Given
        Notification notification = newNotificationWithoutPayments();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( timelineDao.getTimeline( Mockito.anyString()) ).thenReturn( Collections.emptySet() );
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
    void downloadDocumentSuccess() {
        //Given
        Notification notification = newNotificationWithDocs();
        FileData fileStorageResponse = FileData.builder()
                .contentLength( CONTENT_LENGTH )
                .contentType( CONTENT_TYPE )
                .content(InputStream.nullInputStream())
                .build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( fileStorageResponse );
        ResponseEntity<Resource> response = notificationRetrieverService.downloadDocument( IUN, 0 );

        //Then
        assertTrue( response.getStatusCode().is2xxSuccessful() );
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

    @Test
    void listNotificationLegalFactSuccess() {
        //Given
        Set<TimelineElement> timelineElements = new HashSet<>();
        List<String> attachmentKeys = new ArrayList<>();
        attachmentKeys.add( KEY );
        timelineElements.add( TimelineElement.builder()
                .category( TimelineElementCategory.SEND_PAPER_FEEDBACK )
                        .details(new SendPaperFeedbackDetails (
                                SendPaperDetails.builder()
                                        .taxId( USER_ID )
                                        .build(),
                                PhysicalAddress.builder().build(),
                                attachmentKeys ,
                                Collections.emptyList()
                        ))
                .build());

        //When
        Mockito.when( timelineDao.getTimeline( Mockito.anyString() ))
                .thenReturn( timelineElements );
        List<LegalFactsListEntry> legalFactsListEntries = notificationRetrieverService.listNotificationLegalFacts( IUN );

        //Then
        assertNotNull( legalFactsListEntries );
    }

    @Test
    void downloadExtChaLegalFactSuccess() {
        //Given
        String[] urls = new String[1];
        try {
            Path path = Files.createTempFile( null,null );
            urls[0] =  new File(path.toString()).toURI().toURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //When
        Mockito.when( externalChannelClient.getResponseAttachmentUrl( Mockito.any(String[].class) ))
                .thenReturn( urls );
         ResponseEntity<Resource> result = notificationRetrieverService.downloadLegalFact( IUN, EXT_CHA_LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
    }

    @Test
    void downloadLegalFactSuccess() {
        //Given
        NotificationAttachment.Ref ref = NotificationAttachment.Ref.builder()
                .key( KEY )
                .versionToken( VERSION_TOKEN )
                .build();

        FileData fileStorageResponse = FileData.builder()
                .contentLength( CONTENT_LENGTH )
                .contentType( CONTENT_TYPE )
                .content(InputStream.nullInputStream())
                .build();

        //When
        Mockito.when( legalFactMetadata.fromIunAndLegalFactId( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( ref );
        Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( fileStorageResponse );
        ResponseEntity<Resource> result = notificationRetrieverService.downloadLegalFact( IUN, LEGAL_FACT_ID);
        //Then
        assertNotNull( result );
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

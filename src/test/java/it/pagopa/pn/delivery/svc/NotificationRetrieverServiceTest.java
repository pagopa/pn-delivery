package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.StatusUtils;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRetrieverServiceTest {
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String KEY = "KEY";
    public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    public static final String VERSION_TOKEN = "VERSION_TOKEN";
    public static final NotificationAttachment NOTIFICATION_ATTACHMENT = NotificationAttachment.builder()
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

    private AttachmentService attachmentService;
    private S3PresignedUrlService s3PresignedUrlService;
    private NotificationViewedProducer notificationViewedProducer;
    private NotificationDao notificationDao;
    private TimelineDao timelineDao;
    private StatusUtils statusUtils;
    private NotificationRetrieverService notificationRetrieverService;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));
        attachmentService = Mockito.mock( AttachmentService.class );
        s3PresignedUrlService = Mockito.mock( S3PresignedUrlService.class );
        notificationViewedProducer = Mockito.mock( NotificationViewedProducer.class );
        notificationDao = Mockito.mock( NotificationDao.class );
        timelineDao = Mockito.mock( TimelineDao.class );
        statusUtils = Mockito.mock( StatusUtils.class );

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
        Mockito.when( notificationDao.searchNotification(
                Mockito.anyBoolean(),
                Mockito.anyString(),
                Mockito.any( Instant.class),
                Mockito.any( Instant.class),
                Mockito.anyString(),
                Mockito.any(NotificationStatus.class),
                Mockito.anyString())).thenReturn( notificationSearchRowList );
        List<NotificationSearchRow> result = notificationRetrieverService.searchNotification(
                BY_SENDER,
                SENDER_ID,
                START_DATE,
                END_DATE,
                USER_ID,
                NotificationStatus.VIEWED,
                SUBJECT);
        //Then
        assertEquals(notificationSearchRowList, result );
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
    void downloadDocumentSuccess() {
        //Given
        Notification notification = newNotificationWithDocs();
        ResponseEntity<Resource> attachmentResponse = ResponseEntity.ok().build();

        //When
        Mockito.when( notificationDao.getNotificationByIun( Mockito.anyString() ))
                .thenReturn( Optional.of(notification) );
        Mockito.when( attachmentService.loadAttachment( Mockito.any(NotificationAttachment.Ref.class) ) )
                .thenReturn( attachmentResponse );
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
                        NOTIFICATION_ATTACHMENT,
                        NOTIFICATION_ATTACHMENT
                ))
                .build();
    }
}

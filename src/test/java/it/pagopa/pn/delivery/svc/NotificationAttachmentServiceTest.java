package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.AttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverValidator;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationAttachmentServiceTest {
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

    private AttachmentService attachmentService;
    private FileStorage fileStorage;
    private LegalfactsMetadataUtils legalfactsMetadataUtils;
    private NotificationReceiverValidator validator;
    private TimelineDao timelineDao;
    private PnDeliveryConfigs cfg;

    @BeforeEach
    public void setup() {
        fileStorage = Mockito.mock( FileStorage.class );
        legalfactsMetadataUtils = new LegalfactsMetadataUtils();
        validator = Mockito.mock( NotificationReceiverValidator.class );
        timelineDao = Mockito.mock(TimelineDao.class);
        cfg = Mockito.mock( PnDeliveryConfigs.class );

        /*// - Separate Tests
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator() );
        AttachmentService attachmentSaver = new AttachmentService( fileStorage,
                new LegalfactsMetadataUtils(),
                validator,
                timelineDao,
                cfg);*/

        attachmentService = new AttachmentService(
                fileStorage,
                legalfactsMetadataUtils,
                validator,
                timelineDao,
                cfg);
    }

    //@Test
    void buildPreloadFullKeySuccess() {
        //Given
        String paId = "PA_ID";
        String key = "KEY";
        //When
        attachmentService.buildPreloadFullKey( Mockito.anyString(), Mockito.anyString() );

        //Then

        //assertEquals( "preload/" + paId + "/" + key,  );
    }

    @Test
    void loadAttachmentSuccess() {
        //Given
        FileData fileData = FileData.builder()
                .key( KEY )
                .contentType( "Content/Type" )
                .contentLength( BASE64_BODY.length() )
                .content(InputStream.nullInputStream())
                .build();

        //When
        Mockito.when(fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString() )).thenReturn( fileData );
        ResponseEntity<Resource> response = attachmentService.loadAttachment( NOTIFICATION_ATTACHMENT.getRef() );

        //Then
        assertEquals( response.getBody() , new InputStreamResource( fileData.getContent() ));
        assertTrue( response.getStatusCode().is2xxSuccessful() );
        assertTrue( response.getHeaders().containsKey( "Content-Length" ) );
        assertTrue( response.getHeaders().containsKey( "Content-Type" ) );
    }

    //@Test
    void listNotificationLegalFacts() {
        //Given
        String iun = "iun";

        //When
        attachmentService.listNotificationLegalFacts( iun );

        //Then
        Mockito.verify( fileStorage ).getDocumentsListing( Mockito.anyString() );
    }

    //@Test
    void loadLegalFactSuccess() {


        attachmentService.loadLegalfact( Mockito.anyString(), Mockito.anyString() );
    }

    private Notification newNotificationWithoutPayments( ) {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType( ServiceLevelType.SIMPLE_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NOTIFICATION_ATTACHMENT,
                        NOTIFICATION_ATTACHMENT
                ))
                .build();
    }
}

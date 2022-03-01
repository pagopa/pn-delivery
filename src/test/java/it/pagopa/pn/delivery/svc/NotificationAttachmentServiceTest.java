package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationAttachmentServiceTest {
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

    private AttachmentService attachmentService;
    private FileStorage fileStorage;
    private NotificationReceiverValidator validator;
    private TimelineDao timelineDao;
    private PnDeliveryConfigs cfg;

    @BeforeEach
    public void setup() {
        fileStorage = Mockito.mock( FileStorage.class );
        validator = Mockito.mock( NotificationReceiverValidator.class );
        timelineDao = Mockito.mock(TimelineDao.class);

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
                timelineDao);
    }

    @Test
    void buildPreloadFullKeySuccess() {
        //Given
        String paId = "PA_ID";
        String key = "KEY";
        //When
        String result = attachmentService.buildPreloadFullKey( paId, key );

        //Then
        assertEquals( "preload/" + paId + "/" + key, result );
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
                        NOTIFICATION_INLINE_ATTACHMENT,
                        NOTIFICATION_INLINE_ATTACHMENT
                ))
                .build();
    }
}

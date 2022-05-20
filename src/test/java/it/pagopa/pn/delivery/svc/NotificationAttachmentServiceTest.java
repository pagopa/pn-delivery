package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class NotificationAttachmentServiceTest {

    private NotificationAttachmentService attachmentService;
    private NotificationDao notificationDao;
    private PnSafeStorageClientImpl pnSafeStorageClient;
    private PnMandateClientImpl pnMandateClient;

    @BeforeEach
    public void setup() {
        Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

        notificationDao = Mockito.mock(NotificationDao.class);
        pnSafeStorageClient = Mockito.mock(PnSafeStorageClientImpl.class);
        pnMandateClient = Mockito.mock( PnMandateClientImpl.class );

        attachmentService = new NotificationAttachmentService(
                 pnSafeStorageClient, notificationDao, pnMandateClient);
    }

    @Test
    void putFiles() {
        //Given
        List<PreLoadRequest> list = new ArrayList<>();
        PreLoadRequest request = new PreLoadRequest();
        request.setContentType("application/pdf");
        request.setPreloadIdx("1");
        list.add(request);

        FileCreationResponse response =new FileCreationResponse();
        response.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        response.setSecret("secret");
        response.setKey("filekey");
        response.setUploadUrl("https://url123");

        when(pnSafeStorageClient.createFile(Mockito.any())).thenReturn(response);

        //When
        List<PreLoadResponse> result = attachmentService.putFiles(list);

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void downloadDocumentWithRedirectByIunAndRecIdxAttachName() {
        //Given
        String iun = "iun";
        int recipientidx = 0;
        String attachmentName = "PAGOPA";

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setChecksum("checksum");
        fileDownloadResponse.setContentType("application/pdf");
        fileDownloadResponse.setDocumentType("PN_NOTIFICATION_ATTACHMENTS");
        fileDownloadResponse.setDocumentStatus("PRELOAD");
        fileDownloadResponse.setKey("filekey");
        fileDownloadResponse.setVersionId("v1");
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo();
        fileDownloadInfo.setUrl("https://url123");
        fileDownloadInfo.setRetryAfter(BigDecimal.valueOf(0));
        fileDownloadResponse.setDownload(fileDownloadInfo);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(fileDownloadResponse);

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadDocumentWithRedirectByIunAndRecIdxAttachName(iun, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadDocumentWithRedirectByIunAndDocIndex() {
    }

    @Test
    void downloadDocumentWithRedirectByIunRecUidAttachNameMandateId() {
    }

    private InternalNotification buildNotification(String iun){

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        NotificationRecipient notificationRecipient = new NotificationRecipient();
        notificationRecipient.setTaxId("taxid");
        NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
        NotificationPaymentAttachment notificationPaymentAttachment = new NotificationPaymentAttachment();
        NotificationAttachmentBodyRef notificationAttachmentBodyRef = new NotificationAttachmentBodyRef();
        notificationAttachmentBodyRef.setKey("filekey");
        notificationPaymentAttachment.setRef(notificationAttachmentBodyRef);
        notificationPaymentInfo.setPagoPaForm(notificationPaymentAttachment);
        notificationRecipient.setPayment(notificationPaymentInfo);
        notification.addRecipientsItem(notificationRecipient);
        return notification;
    }
}

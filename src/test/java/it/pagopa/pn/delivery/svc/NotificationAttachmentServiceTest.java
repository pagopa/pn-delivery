package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class NotificationAttachmentServiceTest {

    public static final String X_PAGOPA_PN_CX_ID = "PF-123-abcd-123";
    public static final String PAGOPA = "PAGOPA";
    public static final String F_24 = "F24";
    public static final String F_24_FLAT = "F24_FLAT";
    public static final String F_24_STANDARD = "F24_STANDARD";

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
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID));
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
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24() {
        //Given
        String iun = "iun";
        int recipientidx = 0;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));
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
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24FLAT() {
        //Given
        String iun = "iun";
        int recipientidx = 0;
        String attachmentName = F_24_FLAT;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));
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
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24STANDARD() {
        //Given
        String iun = "iun";
        int recipientidx = 0;
        String attachmentName = F_24_STANDARD;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));
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
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameRecIdxNotFound() {
        //Given
        String iun = "iun";
        int recipientidx = 10;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID + "-bad", attachmentName));
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
        assertThrows(PnInternalException.class, () -> attachmentService.downloadDocumentWithRedirectByIunAndRecIdxAttachName(iun, recipientidx, attachmentName));


        //Then
    }

    @Test
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameNotificationNotFound() {
        //Given
        String iun = "iun";
        int recipientidx = 0;
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;

        Optional<InternalNotification> optNotification = Optional.ofNullable(null);
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
        assertThrows(PnInternalException.class, () -> attachmentService.downloadDocumentWithRedirectByIunAndRecIdxAttachName(iun, recipientidx, F_24));


        //Then
    }


    @Test
    void downloadDocumentWithRedirectByIunAndDocIndex() {
        //Given
        String iun = "iun";
        int docidx = 0;
        String attachmentName = PAGOPA;


        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID));
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
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadDocumentWithRedirectByIunAndDocIndex(iun, docidx);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }


    @Test
    void downloadDocumentWithRedirectByIunRecUidAttachNameMandateId() {
        //Given
        String iun = "iun";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String mandateId = null;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, xPagopaPnCxId));
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
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadDocumentWithRedirectByIunRecUidAttachNameMandateId(iun, xPagopaPnCxId, attachmentName, mandateId);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadDocumentWithRedirectByIunRecUidAttachNameMandateIdWithMandate() {
        //Given
        String iun = "iun";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, internalIdDelegator));
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

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(mandateId);
        internalMandateDto.setDelegate(xPagopaPnCxId);
        internalMandateDto.setDelegator(internalIdDelegator);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(fileDownloadResponse);
        when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(internalMandateDto));


        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadDocumentWithRedirectByIunRecUidAttachNameMandateId(iun, xPagopaPnCxId, attachmentName, mandateId);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }


    @Test
    void downloadDocumentWithRedirectByIunRecUidAttachNameMandateIdWithMandateNotFound() {
        //Given
        String iun = "iun";
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, internalIdDelegator));
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
        when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of());


        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadDocumentWithRedirectByIunRecUidAttachNameMandateId(iun, X_PAGOPA_PN_CX_ID, PAGOPA, mandateId));


        //Then
    }


    private InternalNotification buildNotification(String iun, String taxid) {
        return buildNotification(iun, taxid, PAGOPA);
    }

    private InternalNotification buildNotification(String iun, String taxid, String channel){

        InternalNotification notification = new InternalNotification();
        notification.setIun(iun);
        NotificationRecipient notificationRecipient = new NotificationRecipient();
        notificationRecipient.setTaxId(taxid);
        NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
        NotificationPaymentAttachment notificationPaymentAttachment = new NotificationPaymentAttachment();
        NotificationAttachmentBodyRef notificationAttachmentBodyRef = new NotificationAttachmentBodyRef();
        notificationAttachmentBodyRef.setKey("filekey");
        notificationPaymentAttachment.setRef(notificationAttachmentBodyRef);
        if (channel.equals(PAGOPA))
            notificationPaymentInfo.setPagoPaForm(notificationPaymentAttachment);
        else if (channel.equals(F_24) || channel.equals(F_24_STANDARD))
            notificationPaymentInfo.setF24standard(notificationPaymentAttachment);
        else if (channel.equals(F_24_FLAT))
            notificationPaymentInfo.setF24flatRate(notificationPaymentAttachment);

        notificationRecipient.setPayment(notificationPaymentInfo);
        notification.addRecipientsItem(notificationRecipient);

        NotificationDocument documentItem = new NotificationDocument();
        NotificationAttachmentBodyRef notificationAttachmentBodyRef1 = new NotificationAttachmentBodyRef();
        notificationAttachmentBodyRef1.setKey("filekey");
        documentItem.setRef(notificationAttachmentBodyRef1);
        documentItem.setTitle("titolo");
        notification.addDocumentsItem(documentItem);


        return notification;
    }
}

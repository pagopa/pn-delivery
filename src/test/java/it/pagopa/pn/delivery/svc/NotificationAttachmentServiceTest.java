package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
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
    private CheckAuthComponent checkAuthComponent;

    @BeforeEach
    public void setup() {
        Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

        notificationDao = Mockito.mock(NotificationDao.class);
        pnSafeStorageClient = Mockito.mock(PnSafeStorageClientImpl.class);
        pnMandateClient = Mockito.mock( PnMandateClientImpl.class );
        checkAuthComponent = Mockito.mock( CheckAuthComponent.class );

        attachmentService = new NotificationAttachmentService(
                 pnSafeStorageClient, notificationDao, pnMandateClient, checkAuthComponent);
    }

    @Test
    void preloadDocuments() {
        //Given
        List<PreLoadRequest> list = new ArrayList<>();
        PreLoadRequest request = new PreLoadRequest();
        request.setContentType("application/pdf");
        request.setPreloadIdx("1");
        request.setSha256("the_sha256_base64_encoded");
        list.add(request);

        FileCreationResponse response =new FileCreationResponse();
        response.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
        response.setSecret("secret");
        response.setKey("filekey");
        response.setUploadUrl("https://url123");

        when(pnSafeStorageClient.createFile(Mockito.any(), Mockito.anyString())).thenReturn(response);

        //When
        List<PreLoadResponse> result = attachmentService.preloadDocuments(list);

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachName() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24FLAT() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24_FLAT;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24STANDARD() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24_STANDARD;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID, attachmentName));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmenttWithRedirectByIunAndRecIdxAttachNameRecIdxNotFound() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 10;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID + "-bad", attachmentName));

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName));

    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameNotificationNotFound() {
        //Given
        String iun = "iun";
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(null);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());

        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, cxId, null, recipientidx, attachmentName));

    }


    @Test
    void downloadDocumentWithRedirectByIunAndDocIndex() {
        //Given
        String iun = "iun";
        String cxType = "PF";
        String cxId = X_PAGOPA_PN_CX_ID;
        int docidx = 0;
        String attachmentName = PAGOPA;


        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, X_PAGOPA_PN_CX_ID));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient,
                0
        );


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadDocumentWithRedirect(
                iun, cxType, cxId, null, docidx);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }


    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateId() {
        //Given
        String iun = "iun";
        String cxType = "PF";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String mandateId = null;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, xPagopaPnCxId));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( xPagopaPnCxId )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class )
                )).thenReturn( authorizationOutcome );

        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, xPagopaPnCxId, mandateId, null,  attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandate() {
        //Given
        String iun = "iun";
        String cxType = "PF";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, internalIdDelegator));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( internalIdDelegator )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);


        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setMandateId(mandateId);
        internalMandateDto.setDelegate(xPagopaPnCxId);
        internalMandateDto.setDelegator(internalIdDelegator);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(internalMandateDto));
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class )
        )).thenReturn( authorizationOutcome );


        //When
        NotificationAttachmentDownloadMetadataResponse result = attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, xPagopaPnCxId, mandateId, null, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(iun + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }


    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandateNotFound() {
        //Given
        String iun = "iun";
        String cxType = "PF";
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(iun, internalIdDelegator));

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class )
        )).thenReturn( authorizationOutcome );


        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                iun, cxType, X_PAGOPA_PN_CX_ID, mandateId, null, PAGOPA));

    }


    private InternalNotification buildNotification(String iun, String taxid) {
        return buildNotification(iun, taxid, PAGOPA);
    }

    private FileDownloadResponse buildFileDownloadResponse(){
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
        return fileDownloadResponse;
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
        notification.setRecipientIds(List.of(taxid));


        return notification;
    }
}

package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class NotificationAttachmentServiceTest {

    public static final String X_PAGOPA_PN_CX_ID = "PF-123-abcd-123";
    public static final String PAGOPA = "PAGOPA";
    public static final String F_24 = "F24";
    public static final String F_24_FLAT = "F24_FLAT";
    public static final String F_24_STANDARD = "F24_STANDARD";
    public static final String IUN = "iun";

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
                 pnSafeStorageClient, notificationDao, checkAuthComponent);
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
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24FlatNotNull() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID, F_24_FLAT);

        Optional<InternalNotification> optNotification = Optional.of(notification);

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        NotificationRecipient notificationRecipient = notification.getRecipients().get(0);

        Mockito.verify(pnSafeStorageClient).getFile( notificationRecipient.getPayment().getF24flatRate().getRef().getKey(), false);

        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24StandardNotNull() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID, F_24_STANDARD);
        Optional<InternalNotification> optNotification = Optional.of(notification);

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);
        
        //Then
        NotificationRecipient notificationRecipient = notification.getRecipients().get(0);
        
        Mockito.verify(pnSafeStorageClient).getFile( notificationRecipient.getPayment().getF24standard().getRef().getKey(), false);


        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }
    
    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24FLAT() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24_FLAT;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24STANDARD() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24_STANDARD;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

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
                IUN, cxType, cxId, null, recipientidx, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameRecIdxNotFound() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 10;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID + "-bad", attachmentName));

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );

        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                IUN, cxType, cxId, null, recipientidx, attachmentName));

    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameNotificationNotFound() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = F_24;

        Optional<InternalNotification> optNotification = Optional.ofNullable(null);

        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(buildFileDownloadResponse());

        //When
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                IUN, cxType, cxId, null, recipientidx, attachmentName));

    }

    @Test
    void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameFileNotFound() {
        //Given
        String cxType = "PA";
        String cxId = "paId";
        int recipientidx = 0;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

        NotificationRecipient recipient = NotificationRecipient.builder()
                .taxId( X_PAGOPA_PN_CX_ID )
                .build();

        AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(
                recipient, 0);


        when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
        when(checkAuthComponent.canAccess(
                Mockito.any(ReadAccessAuth.class),
                Mockito.any( InternalNotification.class ) )
        ).thenReturn( authorizationOutcome );
        when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new PnHttpResponseException("test", HttpStatus.NOT_FOUND.value()));

        //Then
        assertThrows(PnBadRequestException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                IUN, cxType, cxId, null, recipientidx, attachmentName));

    }

    @Test
    void downloadDocumentWithRedirectByIunAndDocIndex() {
        //Given
        String cxType = "PF";
        String cxId = X_PAGOPA_PN_CX_ID;
        int docidx = 0;
        String attachmentName = PAGOPA;


        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

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
                IUN, cxType, cxId, null, docidx);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunAndAttachmentNameFailure() {
        //Given
        String cxType = "PF";

        Optional<InternalNotification> optNotification = Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

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
        assertThrows(PnNotFoundException.class, () -> attachmentService.downloadAttachmentWithRedirect(
                IUN, cxType, X_PAGOPA_PN_CX_ID, null, 0 , F_24));

    }

    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateId() {
        //Given
        String cxType = "PF";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String mandateId = null;
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, xPagopaPnCxId));

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
                IUN, cxType, xPagopaPnCxId, mandateId, null,  attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandate() {
        //Given
        String cxType = "PF";
        String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";
        String attachmentName = PAGOPA;

        Optional<InternalNotification> optNotification = Optional.ofNullable(buildNotification(IUN, internalIdDelegator));

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
                IUN, cxType, xPagopaPnCxId, mandateId, null, attachmentName);

        //Then
        assertNotNull(result);
        assertEquals(IUN + "__" +attachmentName + ".pdf",  result.getFilename());
        assertNotNull(result.getUrl());
    }

    @Test
    void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandateNotFound() {
        //Given
        String cxType = "PF";
        String internalIdDelegator = "PF-bcd-123-bcd-123";
        String mandateId = "123-abcd-123456";

        Optional<InternalNotification> optNotification = Optional.of(buildNotification(IUN, internalIdDelegator));

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
                IUN, cxType, X_PAGOPA_PN_CX_ID, mandateId, null, PAGOPA));

    }

    @Test
    void computeFileInfoBadRequestExc() {
        // Given
        InternalNotification notification = buildNotification( IUN, X_PAGOPA_PN_CX_ID );
        NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify = NotificationAttachmentService
                .FileDownloadIdentify.create( 0, 0, PAGOPA );

        PnHttpResponseException exception = new PnHttpResponseException( "error", 404 );

        Mockito.when( attachmentService.getFile( "filekey" ) ).thenThrow( exception );

        Executable todo = () -> attachmentService.computeFileInfo( fileDownloadIdentify, notification );

        Assertions.assertThrows( PnBadRequestException.class, todo );
    }

    @Test
    void computeFileInfoInternalExcNoPayment() {
        // Given
        InternalNotification notification = buildNotification( IUN, X_PAGOPA_PN_CX_ID );
        notification.setRecipients( Collections.singletonList( NotificationRecipient.builder().build() ) );
        NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify = NotificationAttachmentService
                .FileDownloadIdentify.create( null, 0, PAGOPA );

        Executable todo = () -> attachmentService.computeFileInfo( fileDownloadIdentify, notification );

        Assertions.assertThrows( PnInternalException.class, todo );
    }

    @Test
    void computeFileInfoInternalExcInvalidAttachmentName() {
        // Given
        InternalNotification notification = buildNotification( IUN, X_PAGOPA_PN_CX_ID );
        NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify = NotificationAttachmentService
                .FileDownloadIdentify.create( null, 0, "WrongAttachmentName" );

        Executable todo = () -> attachmentService.computeFileInfo( fileDownloadIdentify, notification );

        Assertions.assertThrows( IllegalArgumentException.class, todo );
    }

    @Test
    void computeFileInfoDefaultContentType() {
        // Given
        InternalNotification notification = buildNotification( IUN, X_PAGOPA_PN_CX_ID );
        NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify = NotificationAttachmentService
                .FileDownloadIdentify.create( 0, 0, PAGOPA );

        FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

        Mockito.when( attachmentService.getFile( "filekey" ) ).thenReturn( response );

        NotificationAttachmentService.FileInfos fileInfos = attachmentService.computeFileInfo( fileDownloadIdentify, notification );

        Assertions.assertEquals( "iun__titolo.pdf", fileInfos.getFileName() );
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
        
        NotificationPaymentAttachment notificationPaymentAttachmentF24Flat = NotificationPaymentAttachment.builder()
                .ref(NotificationAttachmentBodyRef.builder()
                        .key("filekeyf24Flat")
                        .build()
                )
                .build();

        NotificationPaymentAttachment notificationPaymentAttachmentF24Standard = NotificationPaymentAttachment.builder()
                .ref(NotificationAttachmentBodyRef.builder()
                        .key("filekeyf24FStandard")
                        .build()
                )
                .build();
        
        if (channel.equals(PAGOPA))
            notificationPaymentInfo.setPagoPaForm(notificationPaymentAttachment);
        else if (channel.equals(F_24) || channel.equals(F_24_STANDARD))
            notificationPaymentInfo.setF24standard(notificationPaymentAttachmentF24Standard);
        else if (channel.equals(F_24_FLAT))
            notificationPaymentInfo.setF24flatRate(notificationPaymentAttachmentF24Flat);

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

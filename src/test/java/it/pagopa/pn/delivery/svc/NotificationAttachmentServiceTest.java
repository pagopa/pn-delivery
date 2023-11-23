package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.F24Response;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDownloadMetadataResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadResponse;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.pnclient.pnf24.PnF24ClientImpl;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class NotificationAttachmentServiceTest {

  public static final String X_PAGOPA_PN_CX_ID = "PF-123-abcd-123";
  public static final String X_PAGOPA_PN_UID = "123-abcd-123";
  public static final String PAGOPA = "PAGOPA";

  public static final String IUN = "iun";

  private NotificationAttachmentService attachmentService;
  private NotificationDao notificationDao;
  private PnSafeStorageClientImpl pnSafeStorageClient;
  private PnMandateClientImpl pnMandateClient;
  private PnF24ClientImpl pnF24Client;
  private PnDeliveryPushClientImpl pnDeliveryPushClient;
  private CheckAuthComponent checkAuthComponent;
  private NotificationViewedProducer notificationViewedProducer;
  private MVPParameterConsumer mvpParameterConsumer;
  private PnDeliveryConfigs cfg;

  @BeforeEach
  public void setup() {
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));

    notificationDao = Mockito.mock(NotificationDao.class);
    pnSafeStorageClient = Mockito.mock(PnSafeStorageClientImpl.class);
    pnF24Client = Mockito.mock(PnF24ClientImpl.class);
    pnDeliveryPushClient = Mockito.mock(PnDeliveryPushClientImpl.class);
    pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
    checkAuthComponent = Mockito.mock(CheckAuthComponent.class);
    notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
    mvpParameterConsumer = Mockito.mock(MVPParameterConsumer.class);
    cfg = Mockito.mock(PnDeliveryConfigs.class);
    attachmentService = new NotificationAttachmentService(pnSafeStorageClient, pnF24Client, pnDeliveryPushClient, notificationDao,
            checkAuthComponent, notificationViewedProducer, mvpParameterConsumer, cfg);
  }

  @Test
  void preloadDocuments() {
    // Given
    List<PreLoadRequest> list = new ArrayList<>();
    PreLoadRequest request = new PreLoadRequest();
    request.setContentType("application/pdf");
    request.setPreloadIdx("1");
    request.setSha256("the_sha256_base64_encoded");
    list.add(request);

    PreLoadRequest f24MetaRequest = PreLoadRequest.builder()
            .contentType("application/json")
            .preloadIdx("2")
            .sha256("metadata-f24-sha256")
            .build();
    list.add(f24MetaRequest);

    FileCreationResponse response = new FileCreationResponse();
    response.setUploadMethod(FileCreationResponse.UploadMethodEnum.POST);
    response.setSecret("secret");
    response.setKey("filekey");
    response.setUploadUrl("https://url123");

    when(pnSafeStorageClient.createFile(Mockito.any(), Mockito.anyString())).thenReturn(response);

    // When
    List<PreLoadResponse> result = attachmentService.preloadDocuments(list);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachName() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = PAGOPA;

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
            attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID, null), null, recipientidx,
                    attachmentName, null, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any(NotificationViewDelegateInfo.class));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachName2() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID, null);
    assertThrows(PnInternalException.class, () ->
            attachmentService.downloadAttachmentWithRedirectWithFileKey(IUN, internalAuthHeader, null, recipientidx,
                    PAGOPA, 1,false));

  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameRecIdxNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 10;
    String attachmentName = PAGOPA;
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID, null);

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID + "-bad", attachmentName));

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();

    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    assertThrows(PnNotFoundException.class,
            () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
                    recipientidx, attachmentName, null, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameNotificationNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;

    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID, null);

    Optional<InternalNotification> optNotification = Optional.empty();

    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());

    // When
    assertThrows(PnNotFoundException.class,
            () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
                    recipientidx, PAGOPA, null, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameFileNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID, null);

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenThrow(new PnHttpResponseException("test", HttpStatus.NOT_FOUND.value()));

    // Then
    assertThrows(PnBadRequestException.class,
            () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
                    recipientidx, PAGOPA, null, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadDocumentWithRedirectByIunAndDocIndex() {
    // Given
    String cxType = "PF";
    int docidx = 0;

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
            attachmentService.downloadDocumentWithRedirect(IUN, new InternalAuthHeader(cxType, X_PAGOPA_PN_CX_ID, X_PAGOPA_PN_UID, null), null, docidx, true);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",
            result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(1))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.isNull());
  }

  @Test
  void downloadDocumentWithRedirectByIunAndDocIndexInternal() {
    // Given
    String cxType = "PF";
    int docidx = 0;


    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
            attachmentService.downloadDocumentWithRedirect(IUN, new InternalAuthHeader(cxType, X_PAGOPA_PN_CX_ID, X_PAGOPA_PN_UID, null), null, docidx, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",
            result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateId() {
    // Given
    String cxType = "PF";
    String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
    String attachmentName = PAGOPA;

    Optional<InternalNotification> optNotification =
            Optional.ofNullable(buildNotification(IUN, xPagopaPnCxId));

    NotificationRecipient recipient = NotificationRecipient.builder().taxId(xPagopaPnCxId).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
            attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, xPagopaPnCxId, X_PAGOPA_PN_UID, null), null,
                    null, attachmentName, null, true);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(1))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.isNull());
  }

  @Test
  void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandate() {
    // Given
    String cxType = "PF";
    String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
    String internalIdDelegator = "PF-bcd-123-bcd-123";
    String mandateId = "123-abcd-123456";
    String attachmentName = PAGOPA;

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, internalIdDelegator));

    NotificationRecipient recipient =
            NotificationRecipient.builder().taxId(internalIdDelegator).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    InternalMandateDto internalMandateDto = new InternalMandateDto();
    internalMandateDto.setMandateId(mandateId);
    internalMandateDto.setDelegate(xPagopaPnCxId);
    internalMandateDto.setDelegator(internalIdDelegator);

    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), any(), any()))
            .thenReturn(List.of(internalMandateDto));
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);


    // When
    NotificationAttachmentDownloadMetadataResponse result =
            attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, xPagopaPnCxId, X_PAGOPA_PN_UID, null), mandateId,
                    null, attachmentName, null, true);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(1))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateIdWithMandateNotFound() {
    // Given
    String cxType = "PF";
    String internalIdDelegator = "PF-bcd-123-bcd-123";
    String mandateId = "123-abcd-123456";
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, X_PAGOPA_PN_CX_ID, X_PAGOPA_PN_UID, null);

    Optional<InternalNotification> optNotification =
            Optional.of(buildNotification(IUN, internalIdDelegator));

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();


    when(notificationDao.getNotificationByIun( Mockito.anyString(), Mockito.anyBoolean())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
            .thenReturn(buildFileDownloadResponse());
    when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString(), any(), any()))
            .thenReturn(List.of());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
            Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);


    // When
    assertThrows(PnNotFoundException.class,
            () -> attachmentService.downloadAttachmentWithRedirect(
                    IUN,
                    internalAuthHeader,
                    mandateId,
                    null,
                    PAGOPA,
                    null,
                    false)
    );

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
            .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void computeFileInfoBadRequestExc() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(0, 0, PAGOPA, null);

    PnHttpResponseException exception = new PnHttpResponseException("error", 404);

    Mockito.when(attachmentService.getFile("filekey")).thenThrow(exception);

    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertThrows(PnBadRequestException.class, todo);
  }

  @Test
  void computeFileInfoInternalExcNoPayment() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    notification.setRecipients(Collections.singletonList(NotificationRecipient.builder().build()));
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 0, PAGOPA, null);

    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertThrows(NullPointerException.class, todo);
  }

  @Test
  void computeFileInfoInternalExcNoPaymentIsMvp() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    notification.setRecipients(Collections.singletonList(NotificationRecipient.builder().build()));
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 0, PAGOPA, null);
    when(mvpParameterConsumer.isMvp(any())).thenReturn(Boolean.TRUE);
    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);


    Assertions.assertThrows(NullPointerException.class, todo);
  }


  @Test
  void computeFileInfoInternalExcInvalidAttachmentName() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 0, "WrongAttachmentName", null);

    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertThrows(PnNotFoundException.class, todo);
  }

  @Test
  void computeFileInfoDefaultContentType() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(0, 0, PAGOPA, null);

    FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

    Mockito.when(attachmentService.getFile("filekey")).thenReturn(response);

    NotificationAttachmentService.FileInfos fileInfos =
            attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertEquals("iun__titolo.pdf", fileInfos.getFileName());
  }

  @Test
  void computeFileInfoDefaultContentType1() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 0, "F24", null);

    FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

    Mockito.when(attachmentService.getFile("filekey")).thenReturn(response);
    NotificationProcessCostResponse cost = new NotificationProcessCostResponse();
    cost.setAmount(200);
    cost.setRefinementDate(OffsetDateTime.parse("2023-09-25T10:00:00Z"));
    cost.setNotificationViewDate(OffsetDateTime.parse("2023-09-25T11:00:00Z"));
    Mockito.when(pnDeliveryPushClient.getNotificationProcessCost(anyString(),anyInt(),any(), anyBoolean(), anyInt())).thenReturn(cost);

    F24Response f24Response = new F24Response();
    f24Response.setRetryAfter(BigDecimal.valueOf(0));
    f24Response.setUrl("url");
    f24Response.setContentType("application/pdf");
    f24Response.setContentLength(new BigDecimal(100));
    f24Response.setSha256("123");
    Mockito.when(cfg.getF24CxId()).thenReturn("pn-delivery");
    Mockito.when(pnF24Client.generatePDF(anyString(),anyString(),any(),anyInt())).thenReturn(f24Response);
    NotificationAttachmentService.FileInfos fileInfos =
            attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertEquals("url", fileInfos.getFileDownloadResponse().getDownload().getUrl());
    Assertions.assertEquals("123", fileInfos.getFileDownloadResponse().getChecksum());
    Assertions.assertEquals(new BigDecimal(100), fileInfos.getFileDownloadResponse().getContentLength());
    Assertions.assertEquals("application/pdf", fileInfos.getFileDownloadResponse().getContentType());
  }

  @Test
  void computeFileInfoF24Null() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 0, "F24", null);

    FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

    Mockito.when(attachmentService.getFile("filekey")).thenReturn(response);
    NotificationProcessCostResponse cost = new NotificationProcessCostResponse();
    cost.setAmount(200);
    cost.setRefinementDate(OffsetDateTime.parse("2023-09-25T10:00:00Z"));
    cost.setNotificationViewDate(OffsetDateTime.parse("2023-09-25T11:00:00Z"));
    Mockito.when(pnDeliveryPushClient.getNotificationProcessCost(anyString(),anyInt(),any(), anyBoolean(), anyInt())).thenReturn(cost);

    F24Response f24Response = new F24Response();
    f24Response.setRetryAfter(BigDecimal.valueOf(0));
    f24Response.setUrl("url");
    Mockito.when(cfg.getF24CxId()).thenReturn("pn-delivery");
    Mockito.when(pnF24Client.generatePDF(anyString(),anyString(),any(),anyInt())).thenReturn(f24Response);

    NotificationRecipient notificationRecipient = new NotificationRecipient();
    notificationRecipient.setTaxId(X_PAGOPA_PN_CX_ID);
    NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
    notificationPaymentInfo.setF24(null);
    notificationRecipient.setPayment(List.of(notificationPaymentInfo));
    notification.setRecipients(List.of(notificationRecipient));


    Assertions.assertThrows(PnNotFoundException.class, () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification));

  }

  @Test
  void computeFileInfoF24Null2() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
            NotificationAttachmentService.FileDownloadIdentify.create(null, 2, "F24", null);

    FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

    Mockito.when(attachmentService.getFile("filekey")).thenReturn(response);
    NotificationProcessCostResponse cost = new NotificationProcessCostResponse();
    cost.setAmount(200);
    cost.setRefinementDate(OffsetDateTime.parse("2023-09-25T10:00:00Z"));
    cost.setNotificationViewDate(OffsetDateTime.parse("2023-09-25T11:00:00Z"));
    Mockito.when(pnDeliveryPushClient.getNotificationProcessCost(anyString(),anyInt(),any(), anyBoolean(), anyInt())).thenReturn(cost);

    F24Response f24Response = new F24Response();
    f24Response.setRetryAfter(BigDecimal.valueOf(0));
    f24Response.setUrl("url");
    Mockito.when(cfg.getF24CxId()).thenReturn("pn-delivery");
    Mockito.when(pnF24Client.generatePDF(anyString(),anyString(),any(),anyInt())).thenReturn(f24Response);

    NotificationRecipient notificationRecipient = new NotificationRecipient();
    notificationRecipient.setTaxId(X_PAGOPA_PN_CX_ID);
    NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
    notificationPaymentInfo.setF24(null);
    notificationRecipient.setPayment(List.of(notificationPaymentInfo));
    notification.setRecipients(List.of(notificationRecipient));


    Assertions.assertThrows(PnInternalException.class, () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification));

  }

  private InternalNotification buildNotification(String iun, String taxid) {
    return buildNotification(iun, taxid, PAGOPA);
  }

  private FileDownloadResponse buildFileDownloadResponse() {
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
    fileDownloadResponse.setContentLength(BigDecimal.valueOf(0));
    return fileDownloadResponse;
  }

  private InternalNotification buildNotification(String iun, String taxid, String channel) {

    InternalNotification notification = new InternalNotification();
    notification.setPaFee(0);
    notification.setIun(iun);
    NotificationRecipient notificationRecipient = new NotificationRecipient();
    notificationRecipient.setTaxId(taxid);
    NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
    PagoPaPayment pagoPaPayment = new PagoPaPayment();
    F24Payment f24Payment = new F24Payment();

    it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef notificationAttachmentBodyRef = new it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef();

    notificationAttachmentBodyRef.setKey("filekey");
    pagoPaPayment.setAttachment(new it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment());
    pagoPaPayment.getAttachment().setRef(notificationAttachmentBodyRef);

    f24Payment.setTitle("title");
    f24Payment.setMetadataAttachment(new MetadataAttachment());
    f24Payment.setApplyCost(false);

    if (channel.equals(PAGOPA)) {
      notificationPaymentInfo.setPagoPa(pagoPaPayment);
      notificationPaymentInfo.setF24(f24Payment);
    }

    notificationRecipient.setPayment(List.of(notificationPaymentInfo));
    notification.addRecipientsItem(notificationRecipient);

    it.pagopa.pn.delivery.models.internal.notification.NotificationDocument documentItem = new it.pagopa.pn.delivery.models.internal.notification.NotificationDocument();
    it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef notificationAttachmentBodyRef1 =
            new it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef();
    notificationAttachmentBodyRef1.setKey("filekey");
    documentItem.setRef(notificationAttachmentBodyRef1);
    documentItem.setTitle("titolo");
    notification.addDocumentsItem(documentItem);
    notification.setRecipientIds(List.of(taxid));
    return notification;
  }
}

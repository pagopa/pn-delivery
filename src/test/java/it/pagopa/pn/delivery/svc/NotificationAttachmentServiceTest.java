package it.pagopa.pn.delivery.svc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationResponse;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDownloadMetadataResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentAttachment;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadResponse;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;

class NotificationAttachmentServiceTest {

  public static final String X_PAGOPA_PN_CX_ID = "PF-123-abcd-123";
  public static final String X_PAGOPA_PN_UID = "123-abcd-123";
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
  private NotificationViewedProducer notificationViewedProducer;
  private MVPParameterConsumer mvpParameterConsumer;

  @BeforeEach
  public void setup() {
    Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));

    notificationDao = Mockito.mock(NotificationDao.class);
    pnSafeStorageClient = Mockito.mock(PnSafeStorageClientImpl.class);
    pnMandateClient = Mockito.mock(PnMandateClientImpl.class);
    checkAuthComponent = Mockito.mock(CheckAuthComponent.class);
    notificationViewedProducer = Mockito.mock(NotificationViewedProducer.class);
    mvpParameterConsumer = Mockito.mock(MVPParameterConsumer.class);
    attachmentService = new NotificationAttachmentService(pnSafeStorageClient, notificationDao,
        checkAuthComponent, notificationViewedProducer, mvpParameterConsumer);
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
    assertEquals(1, result.size());
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachName() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = PAGOPA;

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any(NotificationViewDelegateInfo.class));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24;

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24FlatNotNull() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24;

    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID, F_24_FLAT);

    Optional<InternalNotification> optNotification = Optional.of(notification);

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    NotificationRecipient notificationRecipient = notification.getRecipients().get(0);

    Mockito.verify(pnSafeStorageClient)
        .getFile(notificationRecipient.getPayment().getF24flatRate().getRef().getKey(), false);

    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24StandardNotNull() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24;

    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID, F_24_STANDARD);
    Optional<InternalNotification> optNotification = Optional.of(notification);

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    NotificationRecipient notificationRecipient = notification.getRecipients().get(0);

    Mockito.verify(pnSafeStorageClient)
        .getFile(notificationRecipient.getPayment().getF24standard().getRef().getKey(), false);


    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameF24FLAT() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24_FLAT;

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadDocumentWithRedirectByIunAndRecIdxAttachNameF24STANDARD() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24_STANDARD;

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID, attachmentName));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, recipientidx,
            attachmentName, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + attachmentName + ".pdf", result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameRecIdxNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 10;
    String attachmentName = F_24;
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID);

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID + "-bad", attachmentName));

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    assertThrows(PnNotFoundException.class,
        () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
            recipientidx, attachmentName, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameNotificationNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = F_24;
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID);

    Optional<InternalNotification> optNotification = Optional.ofNullable(null);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());

    // When
    assertThrows(PnNotFoundException.class,
        () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
            recipientidx, attachmentName, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadAttachmentWithRedirectByIunAndRecIdxAttachNameFileNotFound() {
    // Given
    String cxType = "PA";
    String cxId = "paId";
    int recipientidx = 0;
    String attachmentName = PAGOPA;
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID);

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenThrow(new PnHttpResponseException("test", HttpStatus.NOT_FOUND.value()));

    // Then
    assertThrows(PnBadRequestException.class,
        () -> attachmentService.downloadAttachmentWithRedirect(IUN, internalAuthHeader, null,
            recipientidx, attachmentName, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadDocumentWithRedirectByIunAndDocIndex() {
    // Given
    String cxType = "PF";
    String cxId = X_PAGOPA_PN_CX_ID;
    int docidx = 0;
    String attachmentName = PAGOPA;


    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadDocumentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, docidx, true);

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
    String cxId = X_PAGOPA_PN_CX_ID;
    int docidx = 0;
    String attachmentName = PAGOPA;


    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadDocumentWithRedirect(IUN, new InternalAuthHeader(cxType, cxId, X_PAGOPA_PN_UID), null, docidx, false);

    // Then
    assertNotNull(result);
    assertEquals(IUN + "__" + optNotification.get().getDocuments().get(0).getTitle() + ".pdf",
        result.getFilename());
    assertNotNull(result.getUrl());

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));
  }

  @Test
  void downloadAttachmentWithRedirectByIunAndAttachmentNameFailure() {
    // Given
    String cxType = "PF";
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, X_PAGOPA_PN_CX_ID, X_PAGOPA_PN_UID);

    Optional<InternalNotification> optNotification =
        Optional.of(buildNotification(IUN, X_PAGOPA_PN_CX_ID));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(X_PAGOPA_PN_CX_ID).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    assertThrows(PnNotFoundException.class, () -> attachmentService
        .downloadAttachmentWithRedirect(IUN, internalAuthHeader, null, 0, F_24, false));

    Mockito.verify(notificationViewedProducer, Mockito.times(0))
        .sendNotificationViewed(Mockito.anyString(), Mockito.any(Instant.class), Mockito.anyInt(), Mockito.any( NotificationViewDelegateInfo.class ));

  }

  @Test
  void downloadAttachmentWithRedirectByIunRecUidAttachNameMandateId() {
    // Given
    String cxType = "PF";
    String xPagopaPnCxId = X_PAGOPA_PN_CX_ID;
    String mandateId = null;
    String attachmentName = PAGOPA;

    Optional<InternalNotification> optNotification =
        Optional.ofNullable(buildNotification(IUN, xPagopaPnCxId));

    NotificationRecipient recipient = NotificationRecipient.builder().taxId(xPagopaPnCxId).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);

    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, xPagopaPnCxId, X_PAGOPA_PN_UID), mandateId,
            null, attachmentName, true);

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
        Optional.ofNullable(buildNotification(IUN, internalIdDelegator));

    NotificationRecipient recipient =
        NotificationRecipient.builder().taxId(internalIdDelegator).build();

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.ok(recipient, 0);


    InternalMandateDto internalMandateDto = new InternalMandateDto();
    internalMandateDto.setMandateId(mandateId);
    internalMandateDto.setDelegate(xPagopaPnCxId);
    internalMandateDto.setDelegator(internalIdDelegator);

    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(List.of(internalMandateDto));
    when(checkAuthComponent.canAccess(Mockito.any(ReadAccessAuth.class),
        Mockito.any(InternalNotification.class))).thenReturn(authorizationOutcome);


    // When
    NotificationAttachmentDownloadMetadataResponse result =
        attachmentService.downloadAttachmentWithRedirect(IUN, new InternalAuthHeader(cxType, xPagopaPnCxId, X_PAGOPA_PN_UID), mandateId,
            null, attachmentName, true);

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
    InternalAuthHeader internalAuthHeader = new InternalAuthHeader(cxType, X_PAGOPA_PN_CX_ID, X_PAGOPA_PN_UID);

    Optional<InternalNotification> optNotification =
        Optional.of(buildNotification(IUN, internalIdDelegator));

    AuthorizationOutcome authorizationOutcome = AuthorizationOutcome.fail();


    when(notificationDao.getNotificationByIun(Mockito.anyString())).thenReturn(optNotification);
    when(pnSafeStorageClient.getFile(Mockito.anyString(), Mockito.anyBoolean()))
        .thenReturn(buildFileDownloadResponse());
    when(pnMandateClient.listMandatesByDelegate(Mockito.anyString(), Mockito.anyString()))
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
        NotificationAttachmentService.FileDownloadIdentify.create(0, 0, PAGOPA);

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
        NotificationAttachmentService.FileDownloadIdentify.create(null, 0, PAGOPA);

    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertThrows(PnNotFoundException.class, todo);
  }

  @Test
  void computeFileInfoInternalExcNoPaymentIsMvp() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    notification.setRecipients(Collections.singletonList(NotificationRecipient.builder().build()));
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
        NotificationAttachmentService.FileDownloadIdentify.create(null, 0, PAGOPA);
    when(mvpParameterConsumer.isMvp(any())).thenReturn(Boolean.TRUE);
    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);


    Assertions.assertThrows(PnInternalException.class, todo);
  }


  @Test
  void computeFileInfoInternalExcInvalidAttachmentName() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
        NotificationAttachmentService.FileDownloadIdentify.create(null, 0, "WrongAttachmentName");

    Executable todo = () -> attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertThrows(IllegalArgumentException.class, todo);
  }

  @Test
  void computeFileInfoDefaultContentType() {
    // Given
    InternalNotification notification = buildNotification(IUN, X_PAGOPA_PN_CX_ID);
    NotificationAttachmentService.FileDownloadIdentify fileDownloadIdentify =
        NotificationAttachmentService.FileDownloadIdentify.create(0, 0, PAGOPA);

    FileDownloadResponse response = new FileDownloadResponse().contentType("WrongContntType");

    Mockito.when(attachmentService.getFile("filekey")).thenReturn(response);

    NotificationAttachmentService.FileInfos fileInfos =
        attachmentService.computeFileInfo(fileDownloadIdentify, notification);

    Assertions.assertEquals("iun__titolo.pdf", fileInfos.getFileName());
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
    notification.setIun(iun);
    NotificationRecipient notificationRecipient = new NotificationRecipient();
    notificationRecipient.setTaxId(taxid);
    NotificationPaymentInfo notificationPaymentInfo = new NotificationPaymentInfo();
    NotificationPaymentAttachment notificationPaymentAttachment =
        new NotificationPaymentAttachment();
    NotificationAttachmentBodyRef notificationAttachmentBodyRef =
        new NotificationAttachmentBodyRef();
    notificationAttachmentBodyRef.setKey("filekey");
    notificationPaymentAttachment.setRef(notificationAttachmentBodyRef);

    NotificationPaymentAttachment notificationPaymentAttachmentF24Flat =
        NotificationPaymentAttachment.builder()
            .ref(NotificationAttachmentBodyRef.builder().key("filekeyf24Flat").build()).build();

    NotificationPaymentAttachment notificationPaymentAttachmentF24Standard =
        NotificationPaymentAttachment.builder()
            .ref(NotificationAttachmentBodyRef.builder().key("filekeyf24FStandard").build())
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
    NotificationAttachmentBodyRef notificationAttachmentBodyRef1 =
        new NotificationAttachmentBodyRef();
    notificationAttachmentBodyRef1.setKey("filekey");
    documentItem.setRef(notificationAttachmentBodyRef1);
    documentItem.setTitle("titolo");
    notification.addDocumentsItem(documentItem);
    notification.setRecipientIds(List.of(taxid));


    return notification;
  }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkError;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.when;

@WebFluxTest(NotificationReworkController.class)
class NotificationReworkControllerTest {

    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService retrieveSvc;
    @MockBean
    private PnDeliveryPushClientImpl deliveryPushClient;
    @MockBean
    private PnSafeStorageClientImpl safeStorageClient;
    @MockBean
    private NotificationReworksDao notificationReworksDao;
    @MockBean
    private PnDeliveryConfigs pnDeliveryConfigs;
    @SpyBean
    private ModelMapper modelMapper;

    @Test
    void notificationRework_success() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.EFFECTIVE_DATE);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.empty());
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_invalidTimelineElementError() {
        ReworkRequest reworkRequest = new ReworkRequest();
        InternalNotification notification = new InternalNotification();
        notification.setRecipients(List.of());
        notification.setNotificationStatus(NotificationStatusV26.EFFECTIVE_DATE);

        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);

        ReworkError error = new ReworkError().cause("INVALID_TIMELINE_ELEMENT").description("Errore timeline");
        ReworkResponse reworkResponse = new ReworkResponse();
        reworkResponse.setErrors(List.of(error));
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(reworkResponse));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/delivery-private/v1/notifications/{iun}/rework", IUN)
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void notificationRework_genericError() {
        ReworkRequest reworkRequest = new ReworkRequest();
        InternalNotification notification = new InternalNotification();
        notification.setRecipients(List.of());
        notification.setNotificationStatus(NotificationStatusV26.EFFECTIVE_DATE);

        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);

        ReworkError error = new ReworkError().cause("GENERIC_ERROR").description("Errore generico");
        ReworkResponse reworkResponse = new ReworkResponse();
        reworkResponse.setErrors(List.of(error));
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(reworkResponse));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/delivery-private/v1/notifications/{iun}/rework", IUN)
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void notificationRework_mono_statusNotAccepted() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.DELIVERING);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.empty());
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_multi_statusNotAccepted() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build(), NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.REFUSED);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.empty());
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_rework_already_present_error() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.DELIVERING);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setStatus("PENDING");
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.just(entity));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_rework_already_present_ok() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.VIEWED);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setStatus("DONE");
        entity.setIdx("2");
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.just(entity));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );
    }

    @Test
    void notificationRework_delivery_push_blocking_error() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.VIEWED);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        ReworkError error = new ReworkError();
        error.setCause("INVALID_TIMELINE_ELEMENT");
        error.setDescription("Causa di prova");
        notificationRework.setErrors(List.of(error));
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setStatus("ERROR");
        entity.setIdx("2");
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.just(entity));
        when(notificationReworksDao.update(any())).thenReturn(Mono.just(entity));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(30));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_delivery_push_non_blocking_error() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.VIEWED);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        ReworkError error = new ReworkError();
        error.setCause("CAUSA DI PROVA");
        error.setDescription("Causa di prova");
        notificationRework.setErrors(List.of(error));
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setStatus("ERROR");
        entity.setIdx("2");
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.just(entity));
        when(notificationReworksDao.update(any())).thenReturn(Mono.just(entity));
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        fileDownloadResponse.setRetentionUntil(OffsetDateTime.now().plusDays(1));
        when(safeStorageClient.getFile(any(), any())).thenReturn(fileDownloadResponse);
        when(pnDeliveryConfigs.getDocumentExpiringDateRange()).thenReturn(10);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isEqualTo(400)
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }

    @Test
    void notificationRework_document_gone() {
        ReworkRequest reworkRequest = new ReworkRequest();
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        reworkRequest.setExpectedStatusCode("RECRN001A");
        reworkRequest.setExpectedDeliveryFailureCause("M01");
        reworkRequest.setReason("Ragione di prova");
        reworkRequest.setPcRetry("PCRETRY_0");
        reworkRequest.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        InternalNotification notification = new InternalNotification();
        NotificationDocument doc = new NotificationDocument();
        doc.setTitle("title");
        doc.setDigests(new NotificationAttachmentDigests());
        NotificationAttachmentBodyRef ref = new NotificationAttachmentBodyRef();
        ref.setKey("key");
        ref.setVersionToken("1");
        doc.setRef(ref);
        doc.setDocIdx("docIdx");

        List<NotificationDocument> documents = List.of(doc);
        notification.setDocuments(documents);
        notification.setRecipients(List.of(NotificationRecipient.builder().build()));
        notification.setNotificationStatus(NotificationStatusV26.VIEWED);
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        ReworkError error = new ReworkError();
        error.setCause("CAUSA DI PROVA");
        error.setDescription("Causa di prova");
        notificationRework.setErrors(List.of(error));
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putIfAbsent(any())).thenReturn(Mono.empty());
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setStatus("ERROR");
        entity.setIdx("2");
        when(notificationReworksDao.findLatestByIun(any())).thenReturn(Mono.just(entity));
        when(notificationReworksDao.update(any())).thenReturn(Mono.just(entity));
        when(safeStorageClient.getFile(any(), any())).thenThrow(new PnHttpResponseException("File gone", 410));
        when(pnDeliveryConfigs.getDocumentExpiringDateRange()).thenReturn(10);


        webTestClient.put()
                .uri( "/delivery-private/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isEqualTo(400)
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse.class );

    }
}
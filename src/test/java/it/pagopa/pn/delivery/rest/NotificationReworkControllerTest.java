package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkResponse;
import it.pagopa.pn.delivery.generated.openapi.server.bo.v1.dto.ReworkRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RequestCheckAarDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @SpyBean
    private ModelMapper modelMapper;

    @Test
    void notificationRework_success() {
        ReworkRequest reworkRequest = new ReworkRequest();
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
        when(retrieveSvc.getNotificationInformation(any(), anyBoolean(), anyBoolean())).thenReturn(notification);
        ReworkResponse notificationRework = new ReworkResponse();
        when(deliveryPushClient.notificationRework(any(), any())).thenReturn(Mono.just(notificationRework));
        when(notificationReworksDao.putItem(any())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri( "/delivery-bo/v1/notifications/{iun}/rework"
                        .replace( "{iun}", IUN ))
                .body(Mono.just(reworkRequest), ReworkRequest.class)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( it.pagopa.pn.delivery.generated.openapi.server.bo.v1.dto.ReworkResponse.class );

    }
}
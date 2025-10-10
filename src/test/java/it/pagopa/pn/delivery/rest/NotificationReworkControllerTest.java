package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void retrieveNotificationRework_returnsAllEntitiesForIun() {
        NotificationReworksEntity entity1 = new NotificationReworksEntity();
        entity1.setReworkId("rw1");
        entity1.setStatus("DONE");
        entity1.setExpectedStatusCodes(List.of("CODE1"));
        entity1.setExpectedDeliveryFailureCause("CAUSE1");
        entity1.setReason("Reason1");
        entity1.setCreatedAt(Instant.now());
        entity1.setUpdatedAt(Instant.now());
        entity1.setInvalidatedTimelineElementIds(List.of("el1", "el2"));

        NotificationReworksEntity entity2 = new NotificationReworksEntity();
        entity2.setReworkId("rw2");
        entity2.setStatus("IN_PROGRESS");
        entity2.setExpectedStatusCodes(List.of("CODE2"));
        entity2.setExpectedDeliveryFailureCause("CAUSE2");
        entity2.setReason("Reason2");
        entity2.setCreatedAt(Instant.now());
        entity2.setUpdatedAt(Instant.now());
        entity2.setInvalidatedTimelineElementIds(List.of("el3"));

        when(notificationReworksDao.findByIun(any(), any(Map.class), any(Integer.class)))
                .thenReturn(Mono.just(Page.create(List.of(entity1, entity2), null)));

        webTestClient.get()
                .uri("/delivery-private/v1/notifications/{iun}/rework", IUN)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReworkItemsResponse.class)
                .value(resp -> {
                    assertThat(resp.getItems()).hasSize(2);
                    assertThat(resp.getItems().get(0).getReworkId()).isEqualTo("rw1");
                    assertThat(resp.getItems().get(1).getReworkId()).isEqualTo("rw2");
                });
    }

    @Test
    void retrieveNotificationRework_returnsEmptyListIfNoEntitiesFound() {
        when(notificationReworksDao.findByIun(any(), any(Map.class), any(Integer.class)))
                .thenReturn(Mono.just(Page.create(Collections.emptyList())));

        webTestClient.get()
                .uri("/delivery-private/v1/notifications/{iun}/rework", IUN)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReworkItemsResponse.class)
                .value(resp -> assertThat(resp.getItems()).isEmpty());
    }

    @Test
    void retrieveNotificationRework_withReworkId_returnsSingleEntity() {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId("rw1");
        entity.setStatus("DONE");
        entity.setExpectedStatusCodes(List.of("CODE1"));
        entity.setExpectedDeliveryFailureCause("CAUSE1");
        entity.setReason("Reason1");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setInvalidatedTimelineElementIds(List.of("el1", "el2"));

        when(notificationReworksDao.findByIunAndReworkId(any(), any()))
                .thenReturn(Mono.just(entity));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/delivery-private/v1/notifications/{iun}/rework")
                        .queryParam("reworkId", "rw1")
                        .build(IUN))
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReworkItemsResponse.class)
                .value(resp -> {
                    assertThat(resp.getItems()).hasSize(1);
                    assertThat(resp.getItems().get(0).getReworkId()).isEqualTo("rw1");
                });
    }

    @Test
    void retrieveNotificationRework_withReworkId_returnsEmptyListIfNotFound() {
        when(notificationReworksDao.findByIunAndReworkId(any(), any()))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/delivery-private/v1/notifications/{iun}/rework")
                        .queryParam("reworkId", "notfound")
                        .build(IUN))
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ReworkItemsResponse.class)
                .value(resp -> assertThat(resp.getItems()).isEmpty());
    }

    @Test
    void retrieveNotificationRework_handlesDaoErrorGracefully() {
        when(notificationReworksDao.findByIun(any(), any(Map.class), any(Integer.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        webTestClient.get()
                .uri("/delivery-private/v1/notifications/{iun}/rework", IUN)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
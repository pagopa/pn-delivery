package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NotificationUpdateStatusDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import it.pagopa.pn.delivery.svc.StatusService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.doThrow;

@WebFluxTest(controllers = {PnStatusController.class})
class PnStatusControllerTest {

    @Autowired
    WebTestClient webTestClient;
    @MockBean
    private StatusService svc;

    @Test
    void updateStatus() {
        NotificationUpdateStatusDto dto = NotificationUpdateStatusDto.builder()
                .iun("iun")
                .build();
        
        webTestClient.post()
                .uri("/" + PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), NotificationUpdateStatusDto.class)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateStatusKo() {
        doThrow(new PnInternalException("exception")).when(svc).updateStatus(Mockito.any());

        NotificationUpdateStatusDto dto = NotificationUpdateStatusDto.builder()
                .iun("iun")
                .build();

        webTestClient.post()
                .uri("/" + PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), NotificationUpdateStatusDto.class)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
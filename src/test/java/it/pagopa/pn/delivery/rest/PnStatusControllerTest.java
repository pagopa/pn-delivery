package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.dto.status.ResponseUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun("iun")
                .build();
        
        webTestClient.post()
                .uri("/" + PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), RequestUpdateStatusDto.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResponseUpdateStatusDto.class);
    }

    @Test
    void updateStatusKo() {
        doThrow(new PnInternalException("exception")).when(svc).updateStatus(Mockito.any());

        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun("iun")
                .build();

        webTestClient.post()
                .uri("/" + PnDeliveryRestConstants.NOTIFICATION_UPDATE_STATUS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), RequestUpdateStatusDto.class)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
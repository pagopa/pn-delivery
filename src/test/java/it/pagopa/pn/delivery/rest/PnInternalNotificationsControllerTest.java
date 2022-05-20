package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@WebFluxTest(controllers = {PnInternalNotificationsController.class})
class PnInternalNotificationsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private StatusService svc;

    @MockBean
    private NotificationRetrieverService retrieveSvc;

    @MockBean
    private PnDeliveryConfigs cfg;

    @MockBean
    private ModelMapperFactory modelMapperFactory;


    @Test
    void getSentNotification() {
    }

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
                .expectStatus().isOk();
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
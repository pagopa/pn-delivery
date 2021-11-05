package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.delivery.svc.PaperNotificationFailedService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(PnPaperNotificationFailedController.class)
class PnPaperNotificationFailedControllerTest {
    private static final String RECIPIENT_ID = "4152";
    private static final String IUN = "IUN";
    private static final String USER_ID = "USER_ID";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private PaperNotificationFailedService service;

    @Test
    void searchPaperNotificationsFailed() {
        PaperNotificationFailed paperNotificationFailed = PaperNotificationFailed.builder()
                .iun(IUN).build();
        List<PaperNotificationFailed> listPaperNot = new ArrayList<>();
        listPaperNot.add(paperNotificationFailed);

        Mockito.when(service.getPaperNotificationsFailed(Mockito.anyString()))
                .thenReturn(listPaperNot);

        webTestClient.get()
                .uri("/delivery/notifications/paper-failed?recipientId=" + RECIPIENT_ID)
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-PagoPA-PN-PA", USER_ID)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(List.class);

        Mockito.verify(service).getPaperNotificationsFailed(Mockito.anyString());
    }
}
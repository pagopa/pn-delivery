package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@WebFluxTest(PnSentNotificationsController.class)
class NotificationSearchForSenderControllerTest {

    private static final String SENDER_ID = "test";
    private static final Instant START_DATE = Instant.parse("2021-09-17T00:00:00.00Z");
    private static final Instant END_DATE = Instant.parse("2021-09-18T00:00:00.00Z");
    private static final NotificationStatus STATUS = NotificationStatus.RECEIVED;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String SUBJECT_REG_EXP = "asd";


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService svc;

    @MockBean
    private PnDeliveryConfigs cfg;

    @Test
    void getSuccess() {
        //Given
        List<NotificationSearchRow> resource = new ArrayList<>();
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .senderId(SENDER_ID)
                .sentAt(Instant.parse("2021-09-17T13:45:28.00Z"))
                .recipientId(RECIPIENT_ID)
                .paNotificationId("123")
                .subject("asdasd")
                .build();
        resource.add(searchRow);

        //When
        Mockito.when(svc.searchNotification(
                Mockito.anyBoolean(),
                Mockito.anyString(),
                Mockito.any(Instant.class),
                Mockito.any(Instant.class),
                Mockito.anyString(),
                Mockito.any(NotificationStatus.class),
                Mockito.anyString())
        ).thenReturn(resource);

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH )
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("recipientId", RECIPIENT_ID)
                                .queryParam("status", STATUS)
                                .queryParam("subjectRegExp", SUBJECT_REG_EXP)
                                .build())
                .accept(MediaType.ALL)
                .header( PnDeliveryRestConstants.PA_ID_HEADER, SENDER_ID)
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(svc).searchNotification(true, SENDER_ID, START_DATE, END_DATE, RECIPIENT_ID, STATUS, SUBJECT_REG_EXP);
    }
}

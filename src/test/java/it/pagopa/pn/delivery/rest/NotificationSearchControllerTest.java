package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
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
import java.util.Collections;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class NotificationSearchControllerTest {

    private static final String SENDER_ID = "test";
    private static final Instant START_DATE = Instant.parse("2021-09-17T00:00:00.000Z");
    private static final Instant END_DATE = Instant.parse("2021-09-18T00:00:00.000Z");
    private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String SUBJECT_REG_EXP = "asd";


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService svc;

    @MockBean
    private PnDeliveryConfigs cfg;

    @Test
    void getSenderSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .senderId(SENDER_ID)
                .sentAt(Instant.parse("2021-09-17T13:45:28.00Z"))
                .recipientId(RECIPIENT_ID)
                .paNotificationId("123")
                .subject("asdasd")
                .build();

        ResultPaginationDto<NotificationSearchRow> result =
        ResultPaginationDto.<NotificationSearchRow>builder()
                .result(Collections.singletonList(searchRow))
                .moreResult(false)
                .nextPagesKey(null).build();
        
        //When
        Mockito.when(svc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

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

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(RECIPIENT_ID)
                .status(STATUS)
                .subjectRegExp(SUBJECT_REG_EXP)
                .size(null)
                .nextPagesKey(null)
                .build();

        Mockito.verify(svc).searchNotification(searchDto);
    }

    
    @Test
    void getReceiverSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .senderId(SENDER_ID)
                .sentAt(Instant.parse("2021-09-17T13:45:28.00Z"))
                .recipientId(RECIPIENT_ID)
                .paNotificationId("123")
                .subject("asdasd")
                .build();

        ResultPaginationDto<NotificationSearchRow> result =
                ResultPaginationDto.<NotificationSearchRow>builder()
                        .result(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();
        
        //When
        Mockito.when(svc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH )
                                //.queryParam( "recipientId", RECIPIENT_ID)
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("senderId", SENDER_ID)
                                .queryParam("status", STATUS)
                                .queryParam("subjectRegExp", SUBJECT_REG_EXP)
                                .build())
                .accept(MediaType.ALL)
                .header( PnDeliveryRestConstants.USER_ID_HEADER, RECIPIENT_ID)
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .senderReceiverId(RECIPIENT_ID)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .filterId(SENDER_ID)
                .status(STATUS)
                .subjectRegExp(SUBJECT_REG_EXP)
                .size(null)
                .nextPagesKey(null)
                .build();
        
        Mockito.verify(svc).searchNotification(searchDto);
    }
}

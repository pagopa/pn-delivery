package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@WebFluxTest(controllers = {PnInternalNotificationsController.class})
class PnInternalNotificationsControllerTest {

    private static final String SENDER_ID = "test";
    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final Integer SIZE = 10;
    private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String RECIPIENT_INTERNAL_ID = "PF-2d74ffe9-aa40-47c2-88ea-9fb171ada637";
    private static final String SUBJECT_REG_EXP = "asd";
    private static final String NEXT_PAGES_KEY = "eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ==";
    private static final String DELEGATOR_ID = "DelegatorId";
    private static final String MANDATE_ID = "mandateId";


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

    @Test
    void searchNotificationsPrivate() {
//Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt(Date.from(Instant.parse("2021-09-17T13:45:28.00Z")))
                .recipients(Collections.singletonList(RECIPIENT_INTERNAL_ID))
                .paProtocolNumber("123")
                .subject("asdasd")
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();

        //When
        Mockito.when(retrieveSvc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

        ModelMapper mapper = new ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
        Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/delivery-private/search")
                                .queryParam( "recipientId", RECIPIENT_INTERNAL_ID)
                                .queryParam( "recipientIdOpaque", "true")
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("senderId", SENDER_ID)
                                .queryParam("status", STATUS)
                                .build())
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .senderReceiverId(RECIPIENT_INTERNAL_ID)
                .receiverIdIsOpaque(true)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .statuses(List.of(STATUS))
                .size(null)
                .nextPagesKey(null)
                .build();


        Mockito.verify(retrieveSvc).searchNotification(searchDto);
    }
}
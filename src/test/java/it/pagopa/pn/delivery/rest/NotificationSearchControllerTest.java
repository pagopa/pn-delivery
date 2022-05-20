package it.pagopa.pn.delivery.rest;




import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class NotificationSearchControllerTest {

    private static final String SENDER_ID = "test";
    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final Integer SIZE = 10;
    private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String SUBJECT_REG_EXP = "asd";
    private static final String NEXT_PAGES_KEY = "eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ==";
    private static final String DELEGATOR_ID = "DelegatorId";
    private static final String MANDATE_ID = "mandateId";


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService svc;

    @MockBean
    private NotificationAttachmentService attachmentService;


    @MockBean
    private PnDeliveryConfigs cfg;

    @MockBean
    private ModelMapperFactory modelMapperFactory;

    @Test
    void getSenderSuccess() {
        //Given
        
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt(Date.from(Instant.parse("2021-09-17T13:45:28.00Z")))
                .recipients( Collections.singletonList( RECIPIENT_ID ) )
                .paProtocolNumber("123")
                .subject("asdasd")
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
        ResultPaginationDto.<NotificationSearchRow,String>builder()
                .result(Collections.singletonList(searchRow))
                .moreResult(false)
                .nextPagesKey(Collections.singletonList( null ))
                .build();
        
        //When
        Mockito.when(svc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

        ModelMapper mapper = new ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
        Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

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
                .accept(MediaType.APPLICATION_JSON)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .filterId(RECIPIENT_ID)
                .status(STATUS)
                .subjectRegExp(SUBJECT_REG_EXP)
                .size(null)
                .nextPagesKey(null)
                .build();

        Mockito.verify(svc).searchNotification(searchDto);
    }

    @Test
    void getSenderNextPageSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt(Date.from(Instant.parse("2021-09-17T13:45:28.00Z")))
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject("asdasd")
                //.group( "group" )
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .result(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(Collections.singletonList( null ))
                        .build();

        //When
        Mockito.when(svc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

        ModelMapper mapper = new ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
        Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH )
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam( "size", SIZE )
                                .queryParam( "nextPagesKey", NEXT_PAGES_KEY )
                                .build())
                .accept(MediaType.ALL)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
                .exchange()
                .expectStatus()
                .isOk();

        //Then
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .size( SIZE )
                .nextPagesKey( NEXT_PAGES_KEY )
                .build();

        Mockito.verify(svc).searchNotification(searchDto);
    }

    
    @Test
    void getReceiverSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt(Date.from(Instant.parse("2021-09-17T13:45:28.00Z")))
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject("asdasd")
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .result(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();
        
        //When
        Mockito.when(svc.searchNotification(Mockito.any(InputSearchNotificationDto.class))).thenReturn(result);

        ModelMapper mapper = new ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
        Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH )
                                //.queryParam( "recipientId", RECIPIENT_ID)
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("mandateId", MANDATE_ID)
                                .queryParam("senderId", SENDER_ID)
                                .queryParam("status", STATUS)
                                .queryParam("subjectRegExp", SUBJECT_REG_EXP)
                                .build())
                .accept(MediaType.ALL)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, RECIPIENT_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .senderReceiverId(RECIPIENT_ID)
                .mandateId( MANDATE_ID )
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .filterId(SENDER_ID)
                .status(STATUS)
                .subjectRegExp(SUBJECT_REG_EXP)
                .size(null)
                .nextPagesKey(null)
                .build();
        
        Mockito.verify(svc).searchNotification(searchDto);
    }
}

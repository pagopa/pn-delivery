package it.pagopa.pn.delivery.rest;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullNotificationSearchResponse;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.*;
import it.pagopa.pn.delivery.svc.search.NotificationSearchService;
import it.pagopa.pn.delivery.utils.ModelMapperConfig;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
@Import(ModelMapperConfig.class)
class NotificationSearchControllerTest {

    private static final String SENDER_ID = "test";
    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final Integer SIZE = 10;
    private static final NotificationStatusV26 STATUS = NotificationStatusV26.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String SUBJECT_REG_EXP = "asd";
    private static final String NEXT_PAGES_KEY = "eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ==";
    private static final String MANDATE_ID = "mandateId";
    public static final List<String> GROUPS = List.of("Group1", "Group2");
    public static final String UID = "Uid";


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationSearchService svc;

    @MockBean
    private InformalNotificationDetailRetrieverStrategy informalNotificationDetailRetrieverStrategy;

    @MockBean
    private LegalNotificationDetailRetrieverStrategy legalNotificationDetailRetrieverStrategy;

    @MockBean
    private NotificationAttachmentService attachmentService;

    @MockBean
    private NotificationQRService qrService;

    @MockBean
    private SenderContactsService senderContactsService;


    @MockBean
    private PnDeliveryConfigs cfg;

    @SpyBean
    private ModelMapper modelMapper;

    @Test
    void getSenderSuccess() {
        //Given
        
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
                .recipients( Collections.singletonList( RECIPIENT_ID ) )
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .mandateId("mandateId")
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
        ResultPaginationDto.<NotificationSearchRow,String>builder()
                .resultsPage(Collections.singletonList(searchRow))
                .moreResult(false)
                .nextPagesKey(Collections.singletonList( null ))
                .build();
        
        //When
        when(svc.searchNotification(any(InputSearchNotificationDto.class), any(), any()))
                .thenReturn(result);

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH )
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("recipientId", RECIPIENT_ID)
                                .queryParam("status", STATUS)
                                .build())
                .accept(MediaType.APPLICATION_JSON)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0)+","+GROUPS.get(1))
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .filterId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .groups( GROUPS )
                .communicationType(NotificationSearchCommunicationType.LEGAL)
                .size(null)
                .nextPagesKey(null)
                .build();

        verify(svc).searchNotification(searchDto, null, null);
    }

    @Test
    void getSenderNextPageSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .group(GROUPS.get( 0 ))
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(Collections.singletonList( null ))
                        .build();

        //When
        when(svc.searchNotification(any(InputSearchNotificationDto.class), any(), any()))
                .thenReturn(result);

        org.modelmapper.ModelMapper mapper = new org.modelmapper.ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, FullNotificationSearchResponse.class );

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
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) + "," + GROUPS.get(1) )
                .exchange()
                .expectStatus()
                .isOk();

        //Then
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .groups( GROUPS )
                .statuses(List.of())
                .communicationType(NotificationSearchCommunicationType.LEGAL)
                .size( SIZE )
                .nextPagesKey( NEXT_PAGES_KEY )
                .build();

        verify(svc).searchNotification(searchDto, null, null);
    }

    
    @Test
    void getReceiverSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();
        
        //When
        when(svc.searchNotification(any(InputSearchNotificationDto.class), eq("PF"), any()))
                .thenReturn(result);

        org.modelmapper.ModelMapper mapper = new org.modelmapper.ModelMapper();
        mapper.createTypeMap( ResultPaginationDto.class, FullNotificationSearchResponse.class );

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
                                .build())
                .accept(MediaType.ALL)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, RECIPIENT_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(false)
                .skipGroupLabelization(true)
                .senderReceiverId(RECIPIENT_ID)
                .mandateId( MANDATE_ID )
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .filterId(SENDER_ID)
                .statuses(List.of())
                .size(null)
                .nextPagesKey(null)
                .communicationType(NotificationSearchCommunicationType.LEGAL)
                .build();
        
        verify(svc).searchNotification(eq(searchDto), eq("PF"), any());
    }

    @Test
    void getReceiverWithInformalCommunicationType() {
        ArgumentCaptor<InputSearchNotificationDto> captor = receiverSearchAndCapture("INFORMAL");
        org.junit.jupiter.api.Assertions.assertEquals(
                NotificationSearchCommunicationType.INFORMAL, captor.getValue().getCommunicationType());
    }

    @Test
    void getReceiverWithAllCommunicationType() {
        ArgumentCaptor<InputSearchNotificationDto> captor = receiverSearchAndCapture("ALL");
        org.junit.jupiter.api.Assertions.assertEquals(
                NotificationSearchCommunicationType.ALL, captor.getValue().getCommunicationType());
    }

    @Test
    void getReceiverWithLegalCommunicationType() {
        ArgumentCaptor<InputSearchNotificationDto> captor = receiverSearchAndCapture("LEGAL");
        org.junit.jupiter.api.Assertions.assertEquals(
                NotificationSearchCommunicationType.LEGAL, captor.getValue().getCommunicationType());
    }

    @Test
    void getReceiverWithoutCommunicationTypeDefaultsToLegal() {
        ArgumentCaptor<InputSearchNotificationDto> captor = receiverSearchAndCapture(null);
        org.junit.jupiter.api.Assertions.assertEquals(
                NotificationSearchCommunicationType.LEGAL, captor.getValue().getCommunicationType());
    }

    @Test
    void getReceiverResponseMapsCommunicationOutcomes() {
        // riga bonaria con dati dinamici: la response deve esporre communicationType e communicationOutcomes
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt(OffsetDateTime.parse("2021-09-17T13:45:28.00Z"))
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .communicationType("INFORMAL")
                .viewed(true)
                .delivered(false)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> result =
                ResultPaginationDto.<NotificationSearchRow, String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();

        Mockito.when(svc.searchNotification(any(InputSearchNotificationDto.class), eq("PF"), any()))
                .thenReturn(result);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/" + PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH)
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("communicationType", "INFORMAL")
                                .build())
                .accept(MediaType.APPLICATION_JSON)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, RECIPIENT_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.resultsPage[0].communicationType").isEqualTo("INFORMAL")
                .jsonPath("$.resultsPage[0].communicationOutcomes.viewed").isEqualTo(true)
                .jsonPath("$.resultsPage[0].communicationOutcomes.delivered").isEqualTo(false);
    }

    private ArgumentCaptor<InputSearchNotificationDto> receiverSearchAndCapture(String communicationType) {
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt(OffsetDateTime.parse("2021-09-17T13:45:28.00Z"))
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> result =
                ResultPaginationDto.<NotificationSearchRow, String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(null).build();

        Mockito.when(svc.searchNotification(any(InputSearchNotificationDto.class), eq("PF"), any()))
                .thenReturn(result);

        webTestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/" + PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH)
                            .queryParam("startDate", START_DATE)
                            .queryParam("endDate", END_DATE);
                    if (communicationType != null) {
                        uriBuilder.queryParam("communicationType", communicationType);
                    }
                    return uriBuilder.build();
                })
                .accept(MediaType.ALL)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, RECIPIENT_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .exchange()
                .expectStatus()
                .isOk();

        ArgumentCaptor<InputSearchNotificationDto> captor = ArgumentCaptor.forClass(InputSearchNotificationDto.class);
        Mockito.verify(svc).searchNotification(captor.capture(), eq("PF"), any());
        return captor;
    }

    @Test
    void searchNotificationDelegatedSuccess() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.fromValue(STATUS.getValue()))
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
                .recipients( Collections.singletonList( RECIPIENT_ID ) )
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .mandateId(MANDATE_ID)
                .build();

        ResultPaginationDto<NotificationSearchRow,String> result =
                ResultPaginationDto.<NotificationSearchRow,String>builder()
                        .resultsPage(Collections.singletonList(searchRow))
                        .moreResult(false)
                        .nextPagesKey(Collections.singletonList( null ))
                        .build();

        //When
        when(svc.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class)))
                .thenReturn(result);

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.NOTIFICATION_RECEIVED_DELEGATED_PATH )
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("recipientId", RECIPIENT_ID)
                                .queryParam("status", STATUS)
                                .build())
                .accept(MediaType.APPLICATION_JSON)
                .header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0)+","+GROUPS.get(1))
                .exchange()
                .expectStatus()
                .isOk()
                // il delegato risponde con LegalNotificationSearchResponse: status tipizzato NotificationStatusV26
                // e nessun campo communicationOutcomes (esclusivo delle righe destinatario/bonarie)
                .expectBody()
                .jsonPath("$.resultsPage[0].notificationStatus").isEqualTo(STATUS.getValue())
                .jsonPath("$.resultsPage[0].mandateId").isEqualTo("mandateId")
                .jsonPath("$.resultsPage[0].communicationOutcomes").doesNotExist();


        InputSearchNotificationDelegatedDto inputSearchNotificationDelegatedDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId("test")
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .group(null)
                .senderId(null)
                .receiverId(RECIPIENT_ID)
                .statuses(List.of(STATUS))
                .size(null)
                .nextPageKey(null)
                .cxGroups(GROUPS)
                .build();

        verify(svc).searchNotificationDelegated(inputSearchNotificationDelegatedDto);
    }

    @Test
    void searchNotificationDelegatedRejectsInformalStatusOnLegalBoundary() {
        // una riga con stato tipico delle bonarie non deve poter attraversare il boundary legale del delegato
        NotificationSearchRow informalRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(UnifiedNotificationStatus.PROCESSING)
                .sender(SENDER_ID)
                .sentAt(OffsetDateTime.parse("2021-09-17T13:45:28.00Z"))
                .recipients(Collections.singletonList(RECIPIENT_ID))
                .paProtocolNumber("123")
                .subject(SUBJECT_REG_EXP)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> result =
                ResultPaginationDto.<NotificationSearchRow, String>builder()
                        .resultsPage(Collections.singletonList(informalRow))
                        .moreResult(false)
                        .nextPagesKey(Collections.singletonList(null))
                        .build();

        Mockito.when(svc.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class)))
                .thenReturn(result);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/" + PnDeliveryRestConstants.NOTIFICATION_RECEIVED_DELEGATED_PATH)
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("recipientId", RECIPIENT_ID)
                                .queryParam("status", STATUS)
                                .build())
                .accept(MediaType.APPLICATION_JSON)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
                .header(PnDeliveryRestConstants.UID_HEADER, UID)
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) + "," + GROUPS.get(1))
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }
}

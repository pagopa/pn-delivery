package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.status.RequestUpdateStatusDto;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doThrow;

@WebFluxTest(controllers = {PnInternalNotificationsController.class})
class PnInternalNotificationsControllerTest {

    private static final String IUN = "IUN";
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
    void searchNotificationsPrivateBySender() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
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
                                .queryParam("senderId", SENDER_ID)
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .queryParam("status", STATUS)
                                .build())
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus()
                .isOk();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .statuses(List.of(STATUS))
                .size(null)
                .maxPageNumber( 1 )
                .nextPagesKey(null)
                .build();


        Mockito.verify(retrieveSvc).searchNotification(searchDto);
    }

    @Test
    void searchNotificationsPrivate() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
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
                .maxPageNumber( 1 )
                .nextPagesKey(null)
                .build();


        Mockito.verify(retrieveSvc).searchNotification(searchDto);
    }

    @Test
    void searchNotificationsPrivateIllegalArgExcFailure() {
        //Given
        NotificationSearchRow searchRow = NotificationSearchRow.builder()
                .iun("202109-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .notificationStatus(STATUS)
                .sender(SENDER_ID)
                .sentAt( OffsetDateTime.parse("2021-09-17T13:45:28.00Z") )
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
                .is5xxServerError();
    }

    @Test
    void searchNotificationsPrivateFailure() {
        Mockito.when( retrieveSvc.searchNotification( Mockito.any( InputSearchNotificationDto.class ) ) ).thenThrow( PnNotFoundException.class );

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/delivery-private/search")
                                .queryParam("startDate", START_DATE)
                                .queryParam("endDate", END_DATE)
                                .build())
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getSentNotificationPrivateSuccess() {
        // Given
        InternalNotification notification = newNotification();


        // When
        Mockito.when( retrieveSvc.getNotificationInformation( IUN, false, true )).thenReturn( notification );

        ModelMapper mapper = new ModelMapper();
        mapper.createTypeMap( InternalNotification.class, SentNotification.class );
        Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, SentNotification.class ) ).thenReturn( mapper );

        webTestClient.get()
                .uri( "/delivery-private/notifications/{iun}".replace( "{iun}", IUN ) )
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus()
                .isOk();
    }

    private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( "pa_02" )
                .notificationStatus( NotificationStatus.ACCEPTED )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type( NotificationDigitalAddress.TypeEnum.PEC )
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .timeline( Collections.singletonList(TimelineElement.builder().build()))
                .notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                        .status( NotificationStatus.ACCEPTED )
                        .build() ) )
                .build(), Collections.emptyMap(), Collections.singletonList( "recipientId" ));
    }
}

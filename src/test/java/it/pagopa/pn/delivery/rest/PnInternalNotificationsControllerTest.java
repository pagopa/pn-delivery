package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@WebFluxTest(controllers = {PnInternalNotificationsController.class})
class PnInternalNotificationsControllerTest {

    private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
    private static final String SENDER_ID = "test";
    private static final String START_DATE = "2021-09-17T00:00:00.000Z";
    private static final String END_DATE = "2021-09-18T00:00:00.000Z";
    private static final Integer SIZE = 10;
    private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
    private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
    private static final String RECIPIENT_INTERNAL_ID = "PF-2d74ffe9-aa40-47c2-88ea-9fb171ada637";
    public static final InternalAuthHeader INTERNAL_AUTH_HEADER = new InternalAuthHeader("PF", RECIPIENT_INTERNAL_ID, null, null);
    private static final String UID = "2d74ffe9-aa40-47c2-88ea-9fb171ada637";
    private static final String SUBJECT_REG_EXP = "asd";
    private static final String NEXT_PAGES_KEY = "eyJlayI6ImNfYjQyOSMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwiaWsiOnsiaXVuX3JlY2lwaWVudElkIjoiY19iNDI5LTIwMjIwNDA1MTEyOCMjZWQ4NGI4YzktNDQ0ZS00MTBkLTgwZDctY2ZhZDZhYTEyMDcwIiwic2VudEF0IjoiMjAyMi0wNC0wNVQwOToyODo0Mi4zNTgxMzZaIiwic2VuZGVySWRfcmVjaXBpZW50SWQiOiJjX2I0MjkjI2VkODRiOGM5LTQ0NGUtNDEwZC04MGQ3LWNmYWQ2YWExMjA3MCJ9fQ==";
    private static final String DELEGATOR_ID = "DelegatorId";
    private static final String MANDATE_ID = "mandateId";
    private static final String REDIRECT_URL = "http://redirectUrl";
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    private static final String FILENAME = "filename.pdf";
    private static final String PA_TAX_ID = "77777777777";
    private static final String NOTICE_CODE = "302000100000019421";
    private static final String ATTACHMENT_NAME = "PAGOPA";
    private static final int DOCUMENT_IDX = 0;
    public static final String AAR_QR_CODE_VALUE = "WFFNVS1ETFFILVRWTVotMjAyMjA5LVYtMV9GUk1UVFI3Nk0wNkI3MTVFXzc5ZTA3NWMwLWIzY2MtNDc0MC04MjExLTllNTBjYTU4NjIzOQ";
    public static final String URL_AAR_QR_VALUE = "https://fake.domain.com/notifica?aar=WFFNVS1ETFFILVRWTVotMjAyMjA5LVYtMV9GUk1UVFI3Nk0wNkI3MTVFXzc5ZTA3NWMwLWIzY2MtNDc0MC04MjExLTllNTBjYTU4NjIzOQ";


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private StatusService svc;

    @MockBean
    private NotificationRetrieverService retrieveSvc;

    @MockBean
    private NotificationPriceService priceService;

    @MockBean
    private NotificationQRService qrService;

    @MockBean
    private NotificationAttachmentService attachmentService;

    @MockBean
    private PnDeliveryConfigs cfg;

    @MockBean
    private ModelMapperFactory modelMapperFactory;



    @Test
    void updateStatus() {
        RequestUpdateStatusDto dto = RequestUpdateStatusDto.builder()
                .iun(IUN)
                .build();

        webTestClient.post()
                .uri("/delivery-private/notifications/update-status" )
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
                .iun(IUN)
                .build();

        webTestClient.post()
                .uri("/delivery-private/notifications/update-status")
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
        Mockito.when(retrieveSvc.searchNotification(any(InputSearchNotificationDto.class), any(), any()))
                .thenReturn(result);

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

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(SENDER_ID)
                .startDate(Instant.parse(START_DATE))
                .endDate(Instant.parse(END_DATE))
                .statuses(List.of(STATUS))
                .size(null)
                .maxPageNumber( 1 )
                .nextPagesKey(null)
                .build();


        Mockito.verify(retrieveSvc).searchNotification(eq(searchDto), any(), any());
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
        Mockito.when(retrieveSvc.searchNotification(Mockito.any(InputSearchNotificationDto.class), any(), any()))
                .thenReturn(result);

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

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
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


        Mockito.verify(retrieveSvc).searchNotification(searchDto, null, null);
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
        Mockito.when(retrieveSvc.searchNotification(any(InputSearchNotificationDto.class), any(), any()))
                .thenReturn(result);

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
        Mockito.when(retrieveSvc.searchNotification(any(InputSearchNotificationDto.class), any(), any()))
                .thenThrow(new PnNotFoundException("test", "test", "test"));

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

    @Test
    void getNotificationCostSuccess(){

        //Given
        NotificationCostResponse costResponse = NotificationCostResponse.builder()
                .iun( "iun" )
                .recipientIdx( 0 )
                .build();

        //When
        Mockito.when( priceService.getNotificationCost( Mockito.anyString(), Mockito.anyString() ) ).thenReturn( costResponse );

        webTestClient.get()
                .uri( "/delivery-private/notifications/{paTaxId}/{noticeCode}"
                        .replace( "{paTaxId}", PA_TAX_ID )
                        .replace( "{noticeCode}", NOTICE_CODE ))
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(NotificationCostResponse.class );

        //Then
        Mockito.verify( priceService ).getNotificationCost( PA_TAX_ID, NOTICE_CODE );
    }

    @Test
    void getNotificationCostFailure(){
        //When
        Mockito.when( priceService.getNotificationCost( Mockito.anyString(), Mockito.anyString() ) ).thenThrow(new PnNotFoundException("test", "test", "test"));

        webTestClient.get()
                .uri( "/delivery-private/notifications/{paTaxId}/{noticeCode}"
                        .replace( "{paTaxId}", PA_TAX_ID )
                        .replace( "{noticeCode}", NOTICE_CODE ))
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getNotificationQRSuccess(){

        //Given
        ResponseCheckAarDto QrResponse = ResponseCheckAarDto.builder()
                .iun( "iun" )
                .build();

        RequestCheckAarDto dto = RequestCheckAarDto.builder()
                .aarQrCodeValue(AAR_QR_CODE_VALUE)
                .recipientInternalId( "recipientInternalId" )
                .recipientType( "PF" )
                .build();

        //When
        Mockito.when( qrService.getNotificationByQR( Mockito.any( RequestCheckAarDto.class ) )).thenReturn( QrResponse );

        webTestClient.post()
                .uri( "/delivery-private/check-aar-qr-code")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), RequestCheckAarDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseCheckAarDto.class );

        //Then
        Mockito.verify( qrService ).getNotificationByQR( dto );
    }


    @Test
    void getNotificationUrlQRSuccess(){

        //Given
        ResponseCheckAarDto QrResponse = ResponseCheckAarDto.builder()
                .iun( "iun" )
                .build();

        RequestCheckAarDto dto = RequestCheckAarDto.builder()
                .aarQrCodeValue(URL_AAR_QR_VALUE)
                .recipientInternalId( "recipientInternalId" )
                .recipientType( "PF" )
                .build();

        //When
        Mockito.when( qrService.getNotificationByQR( Mockito.any( RequestCheckAarDto.class ) )).thenReturn( QrResponse );

        webTestClient.post()
                .uri( "/delivery-private/check-aar-qr-code")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), RequestCheckAarDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseCheckAarDto.class );

        //Then
        Mockito.verify( qrService ).getNotificationByQR( dto );
    }

    @Test
    void getNotificationQRFailure() {
        RequestCheckAarDto dto = RequestCheckAarDto.builder()
                .aarQrCodeValue(AAR_QR_CODE_VALUE)
                .recipientInternalId("recipientInternalId")
                .recipientType("PF")
                .build();

        //When
        Mockito.when(qrService.getNotificationByQR(Mockito.any(RequestCheckAarDto.class))).thenThrow(new PnNotFoundException("test", "test", "test"));

        webTestClient.post()
                .uri("/delivery-private/check-aar-qr-code")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(dto), RequestCheckAarDto.class)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getNotificationAttachmentPrivateSuccess() {
        NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
                .url( REDIRECT_URL )
                .contentType( "application/pdf" )
                .sha256( SHA256_BODY )
                .filename( FILENAME )
                .build();

        Mockito.when( attachmentService.downloadAttachmentWithRedirect(
                Mockito.anyString(),
                Mockito.any( InternalAuthHeader.class ),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.anyBoolean()
        )).thenReturn( response );

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}", IUN)
                                        .replace("{attachmentName}",ATTACHMENT_NAME ))
                                .queryParam( "recipientInternalId", RECIPIENT_INTERNAL_ID )
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( NotificationAttachmentDownloadMetadataResponse.class );

        Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, null, ATTACHMENT_NAME, false );
    }

    @Test
    void getNotificationAttachmentPrivateFailure() {
        Mockito.doThrow( new PnNotFoundException("test", "test", "test") )
                .when( attachmentService )
                .downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, null, ATTACHMENT_NAME, false );

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}", IUN)
                                        .replace("{attachmentName}",ATTACHMENT_NAME ))
                                .queryParam( "recipientInternalId", RECIPIENT_INTERNAL_ID )
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getNotificationDocumentPrivateSuccess() {

        NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
                .url( REDIRECT_URL )
                .contentType( "application/pdf" )
                .sha256( SHA256_BODY )
                .filename( FILENAME )
                .build();

        Mockito.when( attachmentService.downloadDocumentWithRedirect(
                Mockito.anyString(),
                Mockito.any( InternalAuthHeader.class ),
                Mockito.isNull(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
        )).thenReturn( response );

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/received/"+ IUN +"/attachments/documents/"+DOCUMENT_IDX)
                                .queryParam( "recipientInternalId", RECIPIENT_INTERNAL_ID )
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( NotificationAttachmentDownloadMetadataResponse.class );

        Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_IDX, false );
    }

    @Test
    void getNotificationDocumentPrivateWithRetrySuccess() {

        NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
                .url( null )
                .contentType( "application/pdf" )
                .sha256( SHA256_BODY )
                .filename( FILENAME )
                .retryAfter( 1000 )
                .build();

        Mockito.when( attachmentService.downloadDocumentWithRedirect(
                Mockito.anyString(),
                Mockito.any( InternalAuthHeader.class ),
                Mockito.isNull(),
                Mockito.anyInt(),
                Mockito.anyBoolean()
        )).thenReturn( response );

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/received/"+ IUN +"/attachments/documents/"+DOCUMENT_IDX)
                                .queryParam( "recipientInternalId", RECIPIENT_INTERNAL_ID )
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( NotificationAttachmentDownloadMetadataResponse.class );

        Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_IDX, false );
    }

    @Test
    void getNotificationDocumentPrivateFailure() {
        Mockito.doThrow( new PnNotFoundException("test", "test", "test") )
                .when( attachmentService )
                .downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_IDX, false );

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/received/"+ IUN +"/attachments/documents/"+DOCUMENT_IDX)
                                .queryParam( "recipientInternalId", RECIPIENT_INTERNAL_ID )
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isNotFound();
    }
    
    @Test
    void getQuickAccessLinkTokensPrivateSuccess() {

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/"+ IUN +"/quick-access-link-tokens")
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody( Map.class );

        Mockito.verify( qrService ).getQRByIun(IUN);
    }
    
    @Test
    void getQuickAccessLinkTokensPrivateFailure() {
        Mockito.doThrow( new PnNotFoundException("test", "test", "test") )
                .when( qrService )
                .getQRByIun( IUN);

        webTestClient.get()
                .uri( uriBuilder ->
                        uriBuilder
                                .path("/delivery-private/notifications/"+ IUN +"/quick-access-link-tokens")
                                .build())
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId("pa_02")
                .notificationStatus(NotificationStatus.ACCEPTED)
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref(NotificationAttachmentBodyRef.builder()
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
                                .ref(NotificationAttachmentBodyRef.builder()
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
                .timeline(Collections.singletonList(TimelineElement.builder().build()))
                .notificationStatusHistory(Collections.singletonList(NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.ACCEPTED)
                        .build()))
                .build(), Collections.singletonList("recipientId"));
    }
}

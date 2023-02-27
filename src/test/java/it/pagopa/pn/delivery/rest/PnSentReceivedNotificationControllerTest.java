package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_FILEINFONOTFOUND;
import static org.mockito.ArgumentMatchers.*;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class PnSentReceivedNotificationControllerTest {

	private static final String IUN = "IUN";
	private static final String CX_ID = "CX_ID";
	private static final String UID = "UID";
	private static final String PA_ID = "PA_ID";
	private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
	private static final int DOCUMENT_INDEX = 0;
	private static final String REDIRECT_URL = "http://redirectUrl?token=fakeToken";
	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String FILENAME = "filename.pdf";
	private static final String REQUEST_ID = "VkdLVi1VS0hOLVZJQ0otMjAyMjA1LVAtMQ==";
	private static final String MANDATE_ID = "mandateId";
	public static final String CX_TYPE_PF = "PF";
	public static final InternalAuthHeader INTERNAL_AUTH_HEADER = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, List.of("asdasd"));
	private static final String CX_TYPE_PA = "PA";
	private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
	private static final String IDEMPOTENCE_TOKEN = "idempotenceToken";
	private static final String PAGOPA = "PAGOPA";
	public static final String AAR_QR_CODE_VALUE_V1 = "WFFNVS1ETFFILVRWTVotMjAyMjA5LVYtMV9GUk1UVFI3Nk0wNkI3MTVFXzc5ZTA3NWMwLWIzY2MtNDc0MC04MjExLTllNTBjYTU4NjIzOQ";
	public static final String AAR_QR_CODE_VALUE_V2 = "VVFNWi1LTERHLUtEWVQtMjAyMjExLUwtMV9QRi00ZmM3NWRmMy0wOTEzLTQwN2UtYmRhYS1lNTAzMjk3MDhiN2RfZDA2ZjdhNDctNDJkMC00NDQxLWFkN2ItMTE4YmQ4NzlkOTJj";
	private static final String SENDER_ID = "test";
	private static final String START_DATE = "2021-09-17T00:00:00.000Z";
	private static final String END_DATE = "2021-09-18T00:00:00.000Z";
	private static final NotificationStatus STATUS = NotificationStatus.IN_VALIDATION;
	private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
	public static final List<String> GROUPS = List.of("Group1", "Group2");

	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private NotificationAttachmentService attachmentService;

	@MockBean
	private NotificationQRService qrService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@MockBean
	private ModelMapperFactory modelMapperFactory;

	@Test
	void getSentNotificationSuccess() {
		// Given		
		InternalNotification notification = newNotification();
		
		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullSentNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString() ) ).thenReturn( notification );
				
		// Then		
		webTestClient.get()
			.uri( "/delivery/notifications/sent/" + IUN  )
			.accept( MediaType.ALL )
			.header(HttpHeaders.ACCEPT, "application/json")
			.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
			.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
			.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
			.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(FullSentNotification.class);
		
		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck(IUN, PA_ID);
	}

	@Test
	void getSentNotificationNotFoundCauseIN_VALIDATION() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatus( NotificationStatus.IN_VALIDATION );

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullSentNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString() ) ).thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck(IUN, PA_ID);
	}

	@Test
	void getNotificationRequestStatusByRequestIdIN_VALIDATION() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatusHistory( null );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString() ) ).thenReturn( notification );

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, NewNotificationRequestStatusResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, NewNotificationRequestStatusResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponse.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID );
	}

	@Test
	void getNotificationRequestStatusByRequestIdREFUSED() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
						.status( NotificationStatus.REFUSED )
				.build() ) );
		notification.setTimeline( Collections.singletonList( TimelineElement.builder()
						.category( TimelineElementCategory.REQUEST_REFUSED )
						.details( TimelineElementDetails.builder()
								.errors( Collections.singletonList( "Errore" ) )
								.build() )
				.build() ) );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString() ) ).thenReturn( notification );

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, NewNotificationRequestStatusResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, NewNotificationRequestStatusResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponse.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID );
	}

	@Test
	void getNotificationRequestStatusByRequestIdSuccess() {
		// Given
		InternalNotification notification = newNotification();

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString() ) ).thenReturn( notification );

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, NewNotificationRequestStatusResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, NewNotificationRequestStatusResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponse.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID );
	}

	@Test
	void getNotificationRequestStatusByProtocolOnlyFailure() {
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.queryParam("paProtocolNumber", PA_PROTOCOL_NUMBER)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void getNotificationRequestStatusWithoutProtocol() {
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void getNotificationRequestStatusByProtocolAndIdempotenceSuccess() {
		// Given
		InternalNotification notification = newNotification();

		Mockito.when( svc.getNotificationInformation( anyString(), anyString(), anyString() ) ).thenReturn( notification );

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, NewNotificationRequestStatusResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, NewNotificationRequestStatusResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/requests" )
								.queryParam("paProtocolNumber", PA_PROTOCOL_NUMBER)
								.queryParam( "idempotenceToken", IDEMPOTENCE_TOKEN )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponse.class );

		Mockito.verify( svc ).getNotificationInformation( PA_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN );
	}

	@Test
	void getReceivedNotificationSuccess() {
		// Given
		InternalNotification notification = newNotification();
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, List.of("asdasd"));

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullReceivedNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullReceivedNotification.class ) ).thenReturn( mapper );

		Mockito.when(svc.getNotificationAndNotifyViewedEvent(Mockito.anyString(), Mockito.any(InternalAuthHeader.class), eq(null)))
				.thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotification.class);

		Mockito.verify(svc).getNotificationAndNotifyViewedEvent(IUN, internalAuthHeader, null);
	}

	@Test
	void getReceivedNotificationFailure() {

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullReceivedNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullReceivedNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.any( InternalAuthHeader.class ), eq( null )) )
				.thenThrow(new PnNotificationNotFoundException("test"));

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationByDelegateSuccess() {
		// Given
		InternalNotification notification = newNotification();
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, List.of("asdasd"));

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullReceivedNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullReceivedNotification.class ) ).thenReturn( mapper );

		Mockito.when(svc.getNotificationAndNotifyViewedEvent(anyString(), any(InternalAuthHeader.class), anyString()))
				.thenReturn(notification);

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/" + IUN )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotification.class);

		Mockito.verify(svc).getNotificationAndNotifyViewedEvent(IUN, internalAuthHeader, MANDATE_ID);
	}

	@Test
	void getSentNotificationDocumentsWithPresignedSuccess() {
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PA, PA_ID, UID, List.of("asdasd"));

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
						anyString(),
						any(InternalAuthHeader.class),
						isNull(),
						Mockito.anyInt(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, internalAuthHeader, null, DOCUMENT_INDEX, false );
	}

	@Test
	void getReceivedNotificationDocumentsWithPresignedSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
						anyString(),
						any(InternalAuthHeader.class),
						Mockito.isNull(),
						Mockito.anyInt(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_INDEX, true );
	}

	@Test
	void getReceivedNotificationDocumentsWithRetryAfterSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( null )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.retryAfter( 3600 )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
				anyString(),
				any(InternalAuthHeader.class),
				Mockito.isNull(),
				Mockito.anyInt(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
				.isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_INDEX, true );
	}

	@Test
	void getReceivedNotificationDocumentsWithMandateIdSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
				anyString(),
				any(InternalAuthHeader.class),
				anyString(),
				Mockito.anyInt(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID, DOCUMENT_INDEX, true );
	}

	@Test
	void getSentNotificationAttachmentSuccess() {
		//Given
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PA, CX_ID, UID, List.of("asdasd"));

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirect(
						anyString(),
						any(InternalAuthHeader.class),
						isNull(),
						Mockito.anyInt(),
						anyString(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",PAGOPA))
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, internalAuthHeader, null,  0, PAGOPA, false);
	}

	@Test
	void getSentNotificationAttachmentFailure() {
		// When
		Mockito.doThrow( new PnNotificationNotFoundException("Simulated Error") )
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd")), null, 0, PAGOPA, false );

		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getSentNotificationDocumentFailure() {
		// When
		Mockito.when( attachmentService.downloadDocumentWithRedirect( IUN, new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd")), null, 0, false ))
				.thenThrow( new PnNotificationNotFoundException("Simulated Error") );

		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/documents/{docIdx}".replace("{iun}",IUN).replace("{docIdx}","0"))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationDocumentFailure() {
		// When
		Mockito.when( attachmentService.downloadDocumentWithRedirect( IUN, new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd")), null, 0, true ))
				.thenThrow( new PnNotificationNotFoundException("Simulated Error") );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/documents/{docIdx}".replace("{iun}",IUN).replace("{docIdx}","0"))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationAttachmentFailure() {
		// When
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, null);
		Mockito.doThrow( new PnNotificationNotFoundException("Simulated Error") )
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, internalAuthHeader, null,null, PAGOPA, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationAttachmentBadRequestFailure() {
		// When
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, null);
		Mockito.doThrow( new PnBadRequestException("Request took too long to complete.", "test", ERROR_CODE_DELIVERY_FILEINFONOTFOUND))
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, internalAuthHeader, null,null, PAGOPA, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}


	@Test
	void getReceivedNotificationAttachmentInternalErrorFailure() {
		// When
		Mockito.doThrow( new PnInternalException("Simulated Error", "test") )
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null,null, PAGOPA, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.exchange()
				.expectStatus()
				.is5xxServerError();
	}

	@Test
	void getReceivedNotificationAttachmentSuccess() {
		//Given
		String pagopa = "PAGOPA";
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirect(
						anyString(),
						any( InternalAuthHeader.class ),
						anyString(),
						Mockito.isNull(),
						anyString(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",pagopa) )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID, null, pagopa, true);
	}

	@Test
	void getReceivedNotificationAttachmentSuccessNoMandate() {
		//Given
		String pagopa = "PAGOPA";
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirect(
				Mockito.anyString(),
				Mockito.any( InternalAuthHeader.class ),
				Mockito.anyString(),
				Mockito.isNull(),
				Mockito.anyString(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",pagopa) )
								.queryParam("mandateId", "")
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, "", null, pagopa, true);
	}

	@Test
	void searchSentNotificationFailure() {
		// When
		Mockito.doThrow(new PnInternalException("Simulated Error"))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/sent" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.is5xxServerError();

	}

	@Test
	void searchSentNotificationValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/sent" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();

	}

	@Test
	void searchReceivedNotificationValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();

	}

	@Test
	void getNotificationQRV1Success(){

		//Given
		ResponseCheckAarMandateDto QrMandateResponse = ResponseCheckAarMandateDto.builder()
				.iun( "iun" )
				.build();

		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V1)
				.build();

		//When
		Mockito.when( qrService.getNotificationByQRWithMandate(Mockito.any( RequestCheckAarMandateDto.class ), anyString(), anyString(), any()))
				.thenReturn( QrMandateResponse );

		webTestClient.post()
				.uri( "/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(ResponseCheckAarMandateDto.class );

		//Then
		Mockito.verify( qrService ).getNotificationByQRWithMandate( dto, CX_TYPE_PF, CX_ID, null);
	}

	@Test
	void getNotificationQRV2Success(){

		//Given
		ResponseCheckAarMandateDto QrMandateResponse = ResponseCheckAarMandateDto.builder()
				.iun( "iun" )
				.build();

		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V2)
				.build();

		//When
		Mockito.when( qrService.getNotificationByQRWithMandate(Mockito.any( RequestCheckAarMandateDto.class ), anyString(), anyString(), any()))
				.thenReturn( QrMandateResponse );

		webTestClient.post()
				.uri( "/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(ResponseCheckAarMandateDto.class );

		//Then
		Mockito.verify( qrService ).getNotificationByQRWithMandate( dto, CX_TYPE_PF, CX_ID, null);
	}

	@Test
	void getNotificationQRFailure() {
		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V1)
				.build();

		//When
		Mockito.when(qrService.getNotificationByQRWithMandate(Mockito.any(RequestCheckAarMandateDto.class), anyString(), anyString(), any()))
				.thenThrow(new PnNotFoundException("test", "test", "test"));

		webTestClient.post()
				.uri("/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
		headers.add( "Pragma", "no-cache" );
		headers.add( "Expires", "0" );
		return headers;
	}

	private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
				.senderPaId( PA_ID )
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
                .build(), Collections.emptyList(), X_PAGOPA_PN_SRC_CH);
    }
	@Test
	void searchNotificationDelegatedFailure() {
		// When
		Mockito.doThrow(new PnInternalException("Simulated Error"))
				.when(svc)
				.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class));

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

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
				.is5xxServerError();

	}
	@Test
	void searchNotificationDelegatedValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class));

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( ResultPaginationDto.class, NotificationSearchResponse.class );
		Mockito.when( modelMapperFactory.createModelMapper( ResultPaginationDto.class, NotificationSearchResponse.class ) ).thenReturn( mapper );

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
				.isBadRequest();

	}
}

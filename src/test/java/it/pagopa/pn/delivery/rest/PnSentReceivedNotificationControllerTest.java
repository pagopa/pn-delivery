package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class PnSentReceivedNotificationControllerTest {

	private static final String IUN = "IUN";
	private static final String USER_ID = "USER_ID";
	private static final String PA_ID = "PA_ID";
	private static final int DOCUMENT_INDEX = 0;
	private static final String REDIRECT_URL = "http://redirectUrl";
	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String FILENAME = "filename.pdf";
	private static final String REQUEST_ID = "VkdLVi1VS0hOLVZJQ0otMjAyMjA1LVAtMQ==";
	private static final String MANDATE_ID = "mandateId";
	public static final String CX_TYPE_PF = "PF";
	private static final String CX_TYPE_PA = "PA";


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
	void getSentNotificationSuccess() {
		// Given		
		InternalNotification notification = newNotification();
		
		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullSentNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullSentNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationInformation( Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean() ) ).thenReturn( notification );
				
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
		
		Mockito.verify( svc ).getNotificationInformation(IUN, true, true);
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

		Mockito.when( svc.getNotificationInformation( Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean() ) ).thenReturn( notification );

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

		Mockito.verify( svc ).getNotificationInformation(IUN, true, true);
	}

	@Test
	void getNotificationRequestStatusSuccess() {
		// Given
		InternalNotification notification = newNotification();

		Mockito.when( svc.getNotificationInformation( Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean() ) ).thenReturn( notification );

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

		Mockito.verify( svc ).getNotificationInformation( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), true, true );
	}

	@Test
	void getReceivedNotificationSuccess() {
		// Given
		InternalNotification notification = newNotification();

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullReceivedNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullReceivedNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.anyString(), eq( null ) ) )
				.thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotification.class);

		Mockito.verify( svc ).getNotificationAndNotifyViewedEvent(IUN, USER_ID, null);
	}

	@Test
	void getReceivedNotificationByDelegateSuccess() {
		// Given
		InternalNotification notification = newNotification();

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( InternalNotification.class, FullReceivedNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( InternalNotification.class, FullReceivedNotification.class ) ).thenReturn( mapper );

		Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ) )
				.thenReturn( notification );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/" + IUN )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotification.class);

		Mockito.verify( svc ).getNotificationAndNotifyViewedEvent(IUN, USER_ID, MANDATE_ID);
	}

	@Test
	void getSentNotificationDocumentsWithPresignedSuccess() {
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyInt()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, CX_TYPE_PA, PA_ID, null, DOCUMENT_INDEX );
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
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyInt()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, CX_TYPE_PF, USER_ID, null, DOCUMENT_INDEX );
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
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyInt()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
				.isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, CX_TYPE_PF, USER_ID, null, DOCUMENT_INDEX );
	}

	// TODO inserire il test con il mandateID valorizzato

	@Test
	void getSentNotificationAttachmentSuccess() {
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
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyInt(),
						Mockito.anyString()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",pagopa))
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, CX_TYPE_PA, USER_ID, null,  0, pagopa);
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
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyString(),
						Mockito.anyInt(),
						Mockito.anyString()
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
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, CX_TYPE_PF, USER_ID, MANDATE_ID, null, pagopa);
	}

	/*@Test
	void getSentNotificationDocumentsFailure() {
		//Given
		ResponseEntity<Resource> response = ResponseEntity.ok()
				.headers( headers() )
				.contentLength( CONTENT_LENGTH )
				.body( new InputStreamResource( InputStream.nullInputStream() ));

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( svc.downloadDocument( Mockito.anyString(), Mockito.anyInt() ))
				.thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.exchange().expectStatus().is5xxServerError();
	}*/

	/*@Test
	void getReceivedNotificationDocumentsSuccess() {
		//Given
		ResponseEntity<Resource> response = ResponseEntity.ok()
				.headers( headers() )
				.contentLength( CONTENT_LENGTH )
				.contentType( MediaType.APPLICATION_PDF )
				.body( new InputStreamResource( InputStream.nullInputStream() ));

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( svc.downloadDocument( Mockito.anyString(), Mockito.anyInt() ))
				.thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( svc ).downloadDocument( IUN, DOCUMENT_INDEX );
	}*/

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
                .build(), Collections.emptyMap(), Collections.emptyList());
    }
	
}

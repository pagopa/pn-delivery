package it.pagopa.pn.delivery.rest;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;

import static org.junit.jupiter.api.Assertions.assertThrows;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class PnSentReceivedNotificationControllerTest {
	
	private static final String IUN = "IUN";
	private static final String USER_ID = "USER_ID";
	private static final String PA_ID = "PA_ID";
	private static final int DOCUMENT_INDEX = 0;
	private static final String REDIRECT_URL = "http://redirectUrl";
	private static final long CONTENT_LENGTH = 100;

	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private PnDeliveryConfigs cfg;

	@Test
	void getSentNotificationSuccess() {
		// Given		
		Notification notification = newNotification();
		
		// When
		Mockito.when( svc.getNotificationInformation( Mockito.anyString() ) ).thenReturn( notification );
				
		// Then		
		webTestClient.get()
			.uri( "/delivery/notifications/sent/" + IUN  )
			.accept( MediaType.ALL )
			.header(HttpHeaders.ACCEPT, "application/json")
			.header( "X-PagoPA-PN-PA", PA_ID )
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(Notification.class);
		
		Mockito.verify( svc ).getNotificationInformation(IUN, true);
	}

	@Test
	void getReceivedNotificationSuccess() {
		// Given
		Notification notification = newNotification();

		// When
		Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.anyString() ) )
				.thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.USER_ID_HEADER, USER_ID )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(Notification.class);

		Mockito.verify( svc ).getNotificationAndNotifyViewedEvent(IUN, USER_ID);
	}

	@Test
	void getSentNotificationDocumentsWithPresignedSuccess() {

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( svc.downloadDocumentWithRedirect( Mockito.anyString(), Mockito.anyInt() ))
				.thenReturn( REDIRECT_URL );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( "X-PagoPA-PN-PA", PA_ID )
				.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( svc ).downloadDocumentWithRedirect( IUN, DOCUMENT_INDEX );
	}

	@Test
	void getReceivedNotificationDocumentsWithPresignedSuccess() {

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( svc.downloadDocumentWithRedirect( Mockito.anyString(), Mockito.anyInt() ))
				.thenReturn( REDIRECT_URL );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.USER_ID_HEADER, USER_ID )
				.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( svc ).downloadDocumentWithRedirect( IUN, DOCUMENT_INDEX );
	}

	@Test
	void getSentNotificationDocumentsSuccess() {
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
				.uri( "/delivery/notifications/sent/" + IUN + "/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( "X-PagoPA-PN-PA", USER_ID )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( svc ).downloadDocument( IUN, DOCUMENT_INDEX );
	}

	@Test
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
				.header( "X-PagoPA-PN-PA", USER_ID )
				.exchange().expectStatus().is5xxServerError();
	}

	@Test
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
				.header( PnDeliveryRestConstants.USER_ID_HEADER, USER_ID )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( svc ).downloadDocument( IUN, DOCUMENT_INDEX );
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
		headers.add( "Pragma", "no-cache" );
		headers.add( "Expires", "0" );
		return headers;
	}

	private Notification newNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationAttachment.builder()
                                .ref( NotificationAttachment.Ref.builder()
										.key("doc00")
										.versionToken("v01_doc00")
										.build()
								)
								.digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
								.ref( NotificationAttachment.Ref.builder()
										.key("doc01")
										.versionToken("v01_doc01")
										.build()
								)
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
    }
	
}

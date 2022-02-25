package it.pagopa.pn.delivery.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
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

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class PnSentReceivedNotificationLegalFactsControllerTest {
	
	private static final String IUN = "IUN";
	private static final String PA_ID = "PA_ID";
	private static final String USER_ID = "USER_ID";
	private static final String LEGAL_FACT_ID = "LEGAL_FACT_ID";
	private static final long CONTENT_LENGTH = 100;

	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private PnDeliveryConfigs cfg;

	@Test
	void sentNotificationLegalFactsSuccess() {
		// Given
		List<LegalFactsListEntry> list = new ArrayList<>();

		// When
		Mockito.when( svc.listNotificationLegalFacts( IUN ) ).thenReturn( list );

		// Then
		webTestClient.get()
			.uri( "/delivery/notifications/sent/" + IUN + "/legalfacts/" )
			.accept( MediaType.ALL )
			.header(HttpHeaders.ACCEPT, "application/json")
			.header( "X-PagoPA-PN-PA", PA_ID )
			.exchange()
			.expectStatus()
			.isOk()
			.expectBodyList( LegalFactsListEntry.class )
			;

		Mockito.verify( svc ).listNotificationLegalFacts( IUN );
	}

	@Test
	void receivedNotificationLegalFactsSuccess() {
		// Given
		List<LegalFactsListEntry> list = new ArrayList<>();

		// When
		Mockito.when( svc.listNotificationLegalFacts( IUN ) ).thenReturn( list );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/legalfacts/" )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.USER_ID_HEADER, USER_ID )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBodyList( LegalFactsListEntry.class )
		;

		Mockito.verify( svc ).listNotificationLegalFacts( IUN );
	}

	@Test
	void sentNotificationLegalFactSuccess() {
		//Given
		ResponseEntity<Resource> response = ResponseEntity.ok()
				.headers( headers() )
				.contentLength( CONTENT_LENGTH )
				.contentType( MediaType.APPLICATION_PDF )
				.body( new InputStreamResource( InputStream.nullInputStream() ));

		// When
		Mockito.when( svc.downloadLegalFact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID ) ).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/legalfacts/" + LegalFactType.ANALOG_DELIVERY + "/" + LEGAL_FACT_ID )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( "X-PagoPA-PN-PA", PA_ID )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( svc ).downloadLegalFact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID );
	}

	@Test
	void receivedNotificationLegalFactSuccess() {
		//Given
		ResponseEntity<Resource> response = ResponseEntity.ok()
				.headers( headers() )
				.contentLength( CONTENT_LENGTH )
				.contentType( MediaType.APPLICATION_PDF )
				.body( new InputStreamResource( InputStream.nullInputStream() ));

		// When
		Mockito.when( svc.downloadLegalFact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID ) ).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/legalfacts/" + LegalFactType.ANALOG_DELIVERY + "/" + LEGAL_FACT_ID )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.USER_ID_HEADER, USER_ID )
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( svc ).downloadLegalFact( IUN, LegalFactType.ANALOG_DELIVERY, LEGAL_FACT_ID );
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
		headers.add( "Pragma", "no-cache" );
		headers.add( "Expires", "0" );
		return headers;
	}
}

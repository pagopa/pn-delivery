package it.pagopa.pn.delivery.rest;

import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(PnSentNotificationsController.class)
class PnSentNotificationLegalFactsControllerTest {
	
	private static final String IUN = "IUN";
	private static final String PA_ID = "PA_ID";
	
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
	
}

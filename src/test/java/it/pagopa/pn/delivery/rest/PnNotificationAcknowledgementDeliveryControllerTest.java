package it.pagopa.pn.delivery.rest;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.delivery.svc.notificationacknoledgement.NotificationAcknoledgementService;

@WebFluxTest(PnNotificationAcknowledgementDeliveryController.class)
class PnNotificationAcknowledgementDeliveryControllerTest {
	
	private static final String IUN = "IUN";
	private static final int DOCUMENT_INDEX = 0;
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationAcknoledgementService svc;
	
	@Test
	void getSuccess() {
		// Given		
		ResponseEntity<Resource> response = ResponseEntity.ok().build();
				
		// When		
		Mockito.when(svc.notificationAcknowledgement(Mockito.anyString(), Mockito.anyInt())).thenReturn( response );
		
		// Then
		webTestClient.get()
                .uri("/delivery/notifications/acknowledgement/" + IUN + "/" + DOCUMENT_INDEX)
                .accept( MediaType.APPLICATION_JSON )
                .exchange()
                .expectStatus()
                .isOk();
		
		Mockito.verify( svc ).notificationAcknowledgement(IUN, DOCUMENT_INDEX);
	}

}

package it.pagopa.pn.delivery.rest;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.delivery.svc.notificationacknoledgement.NotificationAcknoledgementService;

@WebFluxTest(PnNotificationAcknowledgementDeliveryController.class)
class PnNotificationAcknowledgementDeliveryControllerTest {
	
	private static final String IUN = "IUN";
	private static final int DOCUMENT_INDEX = 0;
	private static final String USER_ID = "USER_ID";
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationAcknoledgementService svc;
	
	@Test
	void getSuccess() {
		// Given		
		ResponseEntity<Resource> resource;
		HttpHeaders headers = new HttpHeaders();
		headers.add( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE );
		resource = ResponseEntity.status( HttpStatus.OK ).headers( headers ).build();
		
		// When
		Mockito.when( svc.notificationAcknowledgement( Mockito.anyString(), Mockito.anyInt(), Mockito.anyString() ) ).thenReturn( resource );
		
		// Then
		webTestClient.get()
                .uri("/delivery/notifications/acknowledgement/" + IUN + "/" + DOCUMENT_INDEX)
                .accept( MediaType.APPLICATION_JSON )
                .header( "X-PagoPA-User-Id", USER_ID)
                .exchange()
                .expectStatus()
                .isOk();
		
		Mockito.verify( svc ).notificationAcknowledgement(IUN, DOCUMENT_INDEX, USER_ID);
	}

}

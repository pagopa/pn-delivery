package it.pagopa.pn.delivery.rest;

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

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.notificationviewed.NotificationViewedService;

@WebFluxTest(PnNotificationViewedDeliveryController.class)
class PnNotificationViewedDeliveryControllerTest {
	
	private static final String IUN = "IUN";
	private static final int DOCUMENT_INDEX = 0;
	private static final String USER_ID = "USER_ID";
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationViewedService svc;
	
	@Test
	void getSuccess() {
		// Given		
		ResponseEntity<Resource> resource;
		HttpHeaders headers = new HttpHeaders();
		headers.add( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE );
		resource = ResponseEntity.status( HttpStatus.OK ).headers( headers ).build();
		
		// When
		Mockito.when( svc.notificationViewed( Mockito.anyString(), Mockito.anyInt(), Mockito.anyString() ) ).thenReturn( resource );
				
		// Then
		webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN + "/documents/" + DOCUMENT_INDEX )
                .accept( MediaType.ALL )
                .header( "X-PagoPA-User-Id", USER_ID)
                .exchange()
                .expectStatus()
                .isOk();
		
		Mockito.verify( svc ).notificationViewed(IUN, DOCUMENT_INDEX, USER_ID);
	}

}

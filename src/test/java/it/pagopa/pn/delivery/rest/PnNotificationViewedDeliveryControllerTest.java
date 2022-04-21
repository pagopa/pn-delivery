package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
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


@WebFluxTest(PnReceivedNotificationsController.class)
class PnNotificationViewedDeliveryControllerTest {
	
	private static final String IUN = "IUN";
	private static final int DOCUMENT_INDEX = 0;
	private static final String USER_ID = "USER_ID";
	
	@Autowired
    private WebTestClient webTestClient;

	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private PnDeliveryConfigs cfg;
	
	@Test
	void getNotificationViewedSuccess() {
		// Given		
		ResponseEntity<Resource> resource;
		HttpHeaders headers = new HttpHeaders();
		headers.add( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE );
		resource = ResponseEntity.status( HttpStatus.OK ).headers( headers ).build();
		
		// When
		Mockito.when(svc.downloadDocument(Mockito.anyString(), Mockito.anyInt())).thenReturn(resource);
				
		// Then
		webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN + "/documents/" + DOCUMENT_INDEX )
                .accept( MediaType.ALL )
                .header(PnDeliveryRestConstants.CX_ID_HEADER, USER_ID )
                .exchange()
                .expectStatus()
                .isOk();

		Mockito.verify(svc).downloadDocument(IUN, DOCUMENT_INDEX);
	}
	
}

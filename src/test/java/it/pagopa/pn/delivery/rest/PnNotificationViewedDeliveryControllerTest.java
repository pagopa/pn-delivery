package it.pagopa.pn.delivery.rest;

import java.util.Arrays;
import java.util.Collections;

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

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
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
	void getNotificationViewedSuccess() {
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
                .header( "X-PagoPA-User-Id", USER_ID )
                .exchange()
                .expectStatus()
                .isOk();
		
		Mockito.verify( svc ).notificationViewed(IUN, DOCUMENT_INDEX, USER_ID);
	}

	@Test
	void getReceivedNotificationSuccess() {
		// Given		
		Notification notification = newNotification();
		
		// When
		Mockito.when( svc.receivedNotification( Mockito.anyString() ) ).thenReturn( notification );
				
		// Then
		webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN )
                .accept( MediaType.ALL )
                .header( "X-PagoPA-User-Id", USER_ID )
                .exchange()
                .expectStatus()
                .isOk();
		
		Mockito.verify( svc ).receivedNotification(IUN);
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
                                .savedVersionId("v01_doc00")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationAttachment.builder()
                                .savedVersionId("v01_doc01")
                                .digests(NotificationAttachment.Digests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .build();
    }
}

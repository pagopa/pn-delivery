package it.pagopa.pn.delivery;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import it.pagopa.pn.delivery.model.notification.NotificationSender;
import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;
import it.pagopa.pn.delivery.rest.PnSentDeliveryController;
import reactor.core.publisher.Mono;

//@RunWith(SpringRunner.class)
@WebFluxTest(PnSentDeliveryController.class)
class PnSentDeliveryControllerTest {
	
	private static final String PA_ID = "paId";
	private static final String IUN = "IUN";
	private static final String PA_NOTIFICATION_ID = "paNotificationId";
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private DeliveryService deliveryService;
	
	@Test
	void testSend_post() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		
		Notification notification = Notification.builder()
				.paNotificationId (PA_NOTIFICATION_ID )
				.sender( notificationSender )
				.recipients( notificationReceipients ).build();
		
		Notification savedNotification = notification.toBuilder().iun( IUN ).build();
				
		// When
		Mockito.when(deliveryService.receiveNotification( ArgumentMatchers.eq(PA_ID), 
				Mockito.any( Notification.class ))).thenReturn( savedNotification );
		
		// Then
		webTestClient.post()
                .uri("/delivery/notifications/sent")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(notification), Notification.class)
                .header("X-PagoPA-PN-PA", PA_ID)
                .exchange()
                .expectStatus().isOk();
		
		Mockito.verify( deliveryService ).receiveNotification( ArgumentMatchers.eq(PA_ID), 
				Mockito.any( Notification.class ));
	}
	
	private NotificationSender getNotificationSender() {
		return NotificationSender.builder().paId(PA_ID)
				.paName("paName").build();
	}

	private List<NotificationRecipient> getNotificationRecipients() {
		PhysicalAddress physicalAddress = new PhysicalAddress();
		physicalAddress.add("physicalAddress");

		return Arrays.asList(NotificationRecipient.builder()
				.digitalDomicile(DigitalAddress.builder().address("address")
				.type(DigitalAddress.Type.PEC).build()).fc("fc")
				.physicalAddress(physicalAddress).build());
	}
	
}

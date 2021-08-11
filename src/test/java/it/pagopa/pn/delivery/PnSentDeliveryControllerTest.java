package it.pagopa.pn.delivery;

import static it.pagopa.pn.delivery.NotificationDtoUtils.buildNotification;
import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

//@WebFluxTest(PnSentDeliveryController.class)
class PnSentDeliveryControllerTest {
	
	private static final String PA_ID = "paId";
	private static final String IUN = "IUN";
	private static final String PA_NOTIFICATION_ID = "paNotificationId";
	
	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationReceiverService deliveryService;
	
	//@Test
	void testSend_post() {
		// Given
		Notification notification = buildNotification( false, PA_ID, PA_NOTIFICATION_ID ).toBuilder()
				.iun( IUN )
				.build();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder().iun( IUN ).build();
				
		// When
		Mockito.when(deliveryService.receiveNotification( Mockito.any( Notification.class )))
				.thenReturn( savedNotification );
		
		// Then
		webTestClient.post()
                .uri("/delivery/notifications/sent")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(notification), NotificationEntity.class)
                .header("X-PagoPA-PN-PA", PA_ID)
                .exchange()
                .expectStatus().isOk();
		
		Mockito.verify( deliveryService ).receiveNotification( Mockito.any( Notification.class ) );
	}

}

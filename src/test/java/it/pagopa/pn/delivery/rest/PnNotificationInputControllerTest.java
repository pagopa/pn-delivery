package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.preload.PreloadRequest;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;

import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(PnNotificationInputController.class)
class PnNotificationInputControllerTest {

	private static final String PA_ID = "paId";
	private static final String IUN = "IUN";
	private static final String PA_NOTIFICATION_ID = "paNotificationId";
	public static final String DOCUMENT_KEY = "doc_1";
	public static final Integer MAX_NUMBER_REQUESTS = 1;
	private static final String SECRET = "secret";
	private static final String METHOD = "PUT";
	private static final String URL = "url";

	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationReceiverService deliveryService;

	@MockBean
	private S3PresignedUrlService presignService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@Test
	void postSuccess() {
		// Given
		Notification notification = Notification.builder()
				.paNotificationId( PA_NOTIFICATION_ID )
				.build();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder().notificationId( EncodingUtils.base64Encoding(IUN) ).build();
				
		// When
		Mockito.when(deliveryService.receiveNotification( Mockito.any( Notification.class )))
				.thenReturn( savedNotification );
		
		// Then
		webTestClient.post()
                .uri("/delivery/notifications/sent")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(notification), Notification.class)
                .header(PnDeliveryRestConstants.PA_ID_HEADER, PA_ID)
                .exchange()
                .expectStatus().isOk();
		
		Mockito.verify( deliveryService ).receiveNotification( Mockito.any( Notification.class ) );
	}

	@Test
	void postPresignedUploadSuccess() {
		// Given
		List<PreloadRequest> requests = new ArrayList<>();
		requests.add( PreloadRequest.builder()
				.key( DOCUMENT_KEY )
				.build());
		List<PreloadResponse> responses = new ArrayList<>();
		responses.add( PreloadResponse.builder()
				.key( DOCUMENT_KEY )
				.secret( SECRET )
				.httpMethod( METHOD )
				.url( URL )
				.build());


		// When
		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );
		Mockito.when(presignService.presignedUpload( Mockito.anyString() , Mockito.anyList() ))
				.thenReturn( responses );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreloadRequest.class)
				.header(PnDeliveryRestConstants.PA_ID_HEADER, PA_ID)
				.exchange()
				.expectStatus().isOk();

		Mockito.verify( presignService ).presignedUpload( PA_ID , requests );
	}

	@Test
	void postPresignedUploadFailure() {
		//GIven
		List<PreloadRequest> requests = new ArrayList<>();
		requests.add( PreloadRequest.builder()
				.key( DOCUMENT_KEY )
				.build());
		requests.add( PreloadRequest.builder()
				.key( DOCUMENT_KEY )
				.build());

		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreloadRequest.class)
				.header(PnDeliveryRestConstants.PA_ID_HEADER, PA_ID)
				.exchange()
				.expectStatus().isBadRequest();

	}

}

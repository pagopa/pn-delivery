package it.pagopa.pn.delivery;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.NotificationAttachment;
import it.pagopa.pn.delivery.model.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import it.pagopa.pn.delivery.model.notification.NotificationSender;
import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;
import it.pagopa.pn.delivery.model.notification.response.NotificationResponse;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;

//@SpringBootTest
class PnSentDeliveryControllerTest {
	
	//@Test
	void testValidNotificationSend() throws URISyntaxException {
	    RestTemplate restTemplate = new RestTemplate();
	    URI uri = new URI("http://localhost:8080/delivery/notifications/send");
	   
	    // Given
	    Notification notification = getNotification();
	    HttpHeaders headers = new HttpHeaders();
	    headers.set("X-PagoPA-PN-PA", "paId"); 
	    HttpEntity<Notification> request = new HttpEntity<>(notification, headers);

	    // When
	    ResponseEntity<NotificationResponse> result = restTemplate.postForEntity(uri, request, NotificationResponse.class);

	    // Then
	    Assert.assertEquals(200, result.getStatusCodeValue());
	    //assertTrue
	}

	private Notification getNotification() {
        return Notification.builder()
                .iun("IUN")
                .paNotificationId("paNot1")
                .cancelledIun("cancellediun")
                .subject("subject 1")
                .cancelledByIun("cancelledByIun1")
                .notificationStatus(NotificationStatus.DELIVERED)
                .notificationStatusHistory(Arrays.asList(NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.PAID)
                        .activeFrom(Instant.now().truncatedTo(ChronoUnit.MILLIS))
                        .build()))
                .recipients(Arrays.asList(NotificationRecipient.builder()
                        .digitalDomicile(DigitalAddress.builder()
                                .address("via Roma 122")
                                .type(DigitalAddress.Type.PEC).build())
                        .physicalAddress(new PhysicalAddress())
                        .fc("fc1")
                        .build()))
                .payment(NotificationPaymentInfo.builder()
                        .iuv("iuv1")
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .digital(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .flatRate(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .build())
                        .notificationFeePolicy(NotificationPaymentInfo.FeePolicies.DELIVERY_MODE)
                        .build())
                .documents(Arrays.asList(NotificationAttachment.builder()
                        .body("body1")
                        .contentType("contentType1")
                        .build()))
                .sender(NotificationSender.builder()
                        .paId("paId1")
                        .paName("paName1")
                        .build())
                .timeline(Arrays.asList(TimelineElement.builder().timestamp(Instant.now().truncatedTo(ChronoUnit.MILLIS)).build()))
                .build();
	}
}

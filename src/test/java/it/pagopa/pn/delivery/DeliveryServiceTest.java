package it.pagopa.pn.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import it.pagopa.pn.commons.kafka.KafkaConfigs;
import it.pagopa.pn.delivery.model.message.Message;
import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.NotificationRecipient;
import it.pagopa.pn.delivery.model.notification.NotificationSender;
import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;
import it.pagopa.pn.delivery.repository.NotificationRepository;

class DeliveryServiceTest {
	
	public NotificationRepository notificationRepository;
	public KafkaTemplate<String, Message> kafkaTemplate;
	public KafkaConfigs kafkaConfigs;
	
	@BeforeEach
	@SuppressWarnings("unchecked")
    public void setup() {
		notificationRepository = Mockito.mock(NotificationRepository.class);
		kafkaTemplate = (KafkaTemplate<String, Message>) Mockito.mock(KafkaTemplate.class);
		kafkaConfigs = Mockito.mock(KafkaConfigs.class);
    }

	@Test
	void testReceiveNotification_valid() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();
		
		// When
		Notification addedNotification = deliveryService.receiveNotification(paId, notification);

		// Then
		Mockito.verify(notificationRepository).save(notification);
		//Mockito.verify(notificationRepository).delete(notification);
	}
	
	@Test
	void testReceiveNotification_nullSender_nullPointerexception() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		NotificationSender notificationSender = null;
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();
		
		// When
		// Notification addedNotification = deliveryService.receiveNotification(paId, notification);
		Throwable throwable = assertThrows(NullPointerException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(NullPointerException.class, throwable.getClass());
	}
	
	@Test
	void testReceiveNotification_noSender_nullPointerexception() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.recipients(notificationReceipients)
										.build();
		
		// When
		Throwable throwable = assertThrows(NullPointerException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(NullPointerException.class, throwable.getClass());
	}
	
	@Test
	void testReceiveNotification_blankpaId_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}
	
	@Test
	void testReceiveNotification_nullpaId_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = null;
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_blankpaNotificationId_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "";
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_nullpaNotificationId_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = null;
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_nopaNotificationId_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder()
										.sender(notificationSender)
										.recipients(notificationReceipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_emptyRecipients_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> receipients = new ArrayList<NotificationRecipient>();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(receipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_nullRecipients_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		ArrayList<NotificationRecipient> receipients = null;
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.recipients(receipients)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	@Test
	void testReceiveNotification_noRecipients_illegalArgumentException() {
		// Given
		DeliveryService deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
		String paNotificationId = "paNotificationId";
		String paId = "paid";
		NotificationSender notificationSender = getNotificationSender();
		Notification notification = Notification.builder()
										.paNotificationId(paNotificationId)
										.sender(notificationSender)
										.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class, () -> deliveryService.receiveNotification(paId, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());	
	}
	
	private NotificationSender getNotificationSender() {
		return NotificationSender.builder().paId("paId").paName("paName").build();
	}
	
	private List<NotificationRecipient> getNotificationRecipients() {
		return Arrays.asList(NotificationRecipient.builder()
				.digitalDomicile(DigitalAddress.builder().address("address").type(DigitalAddress.Type.PEC).build())
				.physicalAddress(new PhysicalAddress()).fc("physicalAddress").build());
	}
	
}

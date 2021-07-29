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
	public DeliveryService deliveryService;
	
	public static final String PA_NOTIFICATIN_ID = "paNotificationId";
	public static final String PA_ID = "paid";

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		notificationRepository = Mockito.mock(NotificationRepository.class);
		kafkaTemplate = (KafkaTemplate<String, Message>) Mockito.mock(KafkaTemplate.class);
		kafkaConfigs = Mockito.mock(KafkaConfigs.class);
		deliveryService = new DeliveryService(Clock.systemUTC(), notificationRepository, kafkaTemplate, kafkaConfigs);
	}

	@Test
	void testReceiveNotification_valid() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Notification addedNotification = deliveryService.receiveNotification(PA_ID, notification);

		// Then
		Mockito.verify(notificationRepository).save(notification);
		// Mockito.verify(notificationRepository).delete(notification);
	}

	@Test
	void testReceiveNotification_nullSender_nullPointerexception() {
		// Given
		NotificationSender notificationSender = null;
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		// Notification addedNotification = deliveryService.receiveNotification(paId, notification);
		Throwable throwable = assertThrows(NullPointerException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(NullPointerException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_noSender_nullPointerexception() {
		// Given
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(NullPointerException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(NullPointerException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_blankpaId_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification("", notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullpaId_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(null, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_blankpaNotificationId_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId("").sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullpaNotificationId_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().paNotificationId(null).sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nopaNotificationId_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> notificationReceipients = getNotificationRecipients();
		Notification notification = Notification.builder().sender(notificationSender)
				.recipients(notificationReceipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_emptyRecipients_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		List<NotificationRecipient> receipients = new ArrayList<NotificationRecipient>();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(receipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullRecipients_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		ArrayList<NotificationRecipient> receipients = null;
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.recipients(receipients).build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_noRecipients_illegalArgumentException() {
		// Given
		NotificationSender notificationSender = getNotificationSender();
		Notification notification = Notification.builder().paNotificationId(PA_NOTIFICATIN_ID).sender(notificationSender)
				.build();

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification(PA_ID, notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	private NotificationSender getNotificationSender() {
		return NotificationSender.builder().paId("paId").paName("paName").build();
	}

	private List<NotificationRecipient> getNotificationRecipients() {
		PhysicalAddress physicalAddress = new PhysicalAddress();
		physicalAddress.add("physicalAddress");

		return Arrays.asList(NotificationRecipient.builder()
				.digitalDomicile(DigitalAddress.builder().address("address").type(DigitalAddress.Type.PEC).build())
				.fc("fc").physicalAddress(physicalAddress).build());
	}

}

package it.pagopa.pn.delivery;


import java.time.Clock;
import java.util.Collections;

import static it.pagopa.pn.delivery.NotificationDtoUtils.buildNotification;
import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DeliveryServiceTest {

	public NotificationDao notificationDao;
	public NewNotificationProducer notificationEventProducer;
	public DeliveryService deliveryService;
	
	public static final String PA_NOTIFICATION_ID = "paNotificationId";
	public static final String PA_ID = "paid";

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() {
		notificationDao = Mockito.mock(NotificationDao.class);
		notificationEventProducer = Mockito.mock(NewNotificationProducer.class);
		deliveryService = new DeliveryService(Clock.systemUTC(), notificationDao, notificationEventProducer );
	}

	@Test
	void testReceiveNotification_valid() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotification = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = buildNotification( false, PA_ID, PA_NOTIFICATION_ID );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( savedNotification.getValue().getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getValue().getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");
	}

	@Test
	void testReceiveNotification_nullSender_nullPointerexception() {
		// Given
		Notification notification = buildNotification( true, null, PA_NOTIFICATION_ID );

		// When
		Throwable throwable = Assertions.assertThrows(NullPointerException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		Assertions.assertEquals(NullPointerException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_blankpaId_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, "", PA_NOTIFICATION_ID );


		// When
		Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		Assertions.assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullpaId_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, null, PA_NOTIFICATION_ID );


		// When
		Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		Assertions.assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_blankpaNotificationId_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, PA_ID, "" );

		// When
		Throwable throwable = Assertions.assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		Assertions.assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullpaNotificationId_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, PA_ID, null );

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_emptyRecipients_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, null, PA_NOTIFICATION_ID, Collections.emptyList() );

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

	@Test
	void testReceiveNotification_nullRecipients_illegalArgumentException() {
		// Given
		Notification notification = buildNotification( false, null, PA_NOTIFICATION_ID, null );

		// When
		Throwable throwable = assertThrows(IllegalArgumentException.class,
				() -> deliveryService.receiveNotification( notification ));

		// Then
		assertEquals(IllegalArgumentException.class, throwable.getClass());
	}

}

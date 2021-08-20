package it.pagopa.pn.delivery.svc.recivenotification;


import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.common.messages.PnValidationException;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.NotificationFactoryForTesting;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

class DeliveryServiceTest {

	private NotificationDao notificationDao;
	private NewNotificationProducer notificationEventProducer;
	private NotificationReceiverService deliveryService;
	private PnDeliveryConfigs configs;
	private FileStorage fileStorage;
	private Clock clock;
	private NewNotificationValidator validator;

	@BeforeEach
	public void setup() {
		notificationDao = Mockito.mock(NotificationDao.class);
		notificationEventProducer = Mockito.mock(NewNotificationProducer.class);
		fileStorage = Mockito.mock( FileStorage.class );
		configs = Mockito.mock( PnDeliveryConfigs.class );

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = new NewNotificationValidator( factory.getValidator() );

		this.clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));
		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				notificationEventProducer,
				fileStorage,
				configs,
				validator
		    );
	}

	@Test
	void testReceiveNotification_valid_with_payments_delivery_mode() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotificationCaptor = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( false );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture() );

		Notification savedNotification = savedNotificationCaptor.getValue();
		assertEquals( savedNotification.getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(4) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).push( Mockito.any( NewNotificationEvent.class) );
	}

	@Test
	void testReceiveNotification_valid_with_payments_flat() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotification = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsFlat( false );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( savedNotification.getValue().getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getValue().getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(3) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).push( Mockito.any( NewNotificationEvent.class) );
	}

	@Test
	void testReceiveNotification_valid_without_payments() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotification = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = NotificationFactoryForTesting.newNotificationWithoutPayments( false );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( savedNotification.getValue().getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getValue().getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(2) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).push( Mockito.any( NewNotificationEvent.class) );
	}


	@Test
	void testReceiveNotification_invalid() {

		// Given
		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( true );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows(PnValidationException.class, todo );
	}

	@Test
	void testReceiveNotification_conflict() throws IdConflictException {
		// Given
		int iunGenerationRetry = 3;
		Mockito.when( configs.getIunRetry() ).thenReturn( iunGenerationRetry );

		Mockito.doThrow( new IdConflictException("IUN") )
				.when( notificationDao )
				.addNotification( Mockito.any( Notification.class) );

		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( false );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows(IllegalStateException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( iunGenerationRetry ) )
				                              .addNotification( Mockito.any( Notification.class ));
	}

	@Test
	void testReceiveNotification_conflict_try_at_least_once_for_negatives() throws IdConflictException {
		// Given
		Mockito.when( configs.getIunRetry() ).thenReturn( 0 );

		Mockito.doThrow( new IdConflictException("IUN") )
				.when( notificationDao )
				.addNotification( Mockito.any( Notification.class) );

		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( false );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows(IllegalStateException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( 1 ) )
				.addNotification( Mockito.any( Notification.class ));
	}

	@Test
	void testReceiveNotification_conflict_try_at_least_once_for_null() throws IdConflictException {
		// Given
		Mockito.when( configs.getIunRetry() ).thenReturn( null );

		Mockito.doThrow( new IdConflictException("IUN") )
				.when( notificationDao )
				.addNotification( Mockito.any( Notification.class) );

		Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( false );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows(IllegalStateException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( 1 ) )
				.addNotification( Mockito.any( Notification.class ));
	}

}

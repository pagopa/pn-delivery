package it.pagopa.pn.delivery.svc.recivenotification;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons_delivery.middleware.NotificationDao;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.svc.AttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import it.pagopa.pn.delivery.svc.NotificationReceiverValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	public static final NotificationAttachment NOTIFICATION_ATTACHMENT = NotificationAttachment.builder()
			.body(BASE64_BODY)
			.contentType("Content/Type")
			.digests(NotificationAttachment.Digests.builder()
					.sha256(SHA256_BODY)
					.build()
			)
			.build();

	private NotificationDao notificationDao;
	private NewNotificationProducer notificationEventProducer;
	private NotificationReceiverService deliveryService;
	private FileStorage fileStorage;

	@BeforeEach
	public void setup() {
		Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

		notificationDao = Mockito.mock(NotificationDao.class);
		notificationEventProducer = Mockito.mock(NewNotificationProducer.class);
		fileStorage = Mockito.mock( FileStorage.class );

		// - Separate Tests
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator() );
		AttachmentService attachmentSaver = new AttachmentService( fileStorage, new LegalfactsMetadataUtils(), validator);

		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				notificationEventProducer,
				attachmentSaver,
				validator
		    );
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithDeliveryModeFee() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotificationCaptor = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture() );

		Notification savedNotification = savedNotificationCaptor.getValue();
		assertEquals( savedNotification.getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(4) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithFlatFee() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotification = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = newNotificationWithPaymentsFlat( );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( savedNotification.getValue().getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getValue().getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(3) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	@Test
	void successWritingNotificationWithoutPaymentsInformation() throws IdConflictException {
		ArgumentCaptor<Notification> savedNotification = ArgumentCaptor.forClass(Notification.class);

		// Given
		Notification notification = newNotificationWithoutPayments( );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( savedNotification.getValue().getIun(), addedNotification.getIun(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getValue().getPaNotificationId(), "Wrong protocol number");
		assertEquals( notification.getPaNotificationId(), addedNotification.getPaNotificationId(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(2) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}


	@Test
	void throwsPnValidationExceptionForInvalidFormatNotification() {

		// Given
		Notification notification = Notification.builder().build();

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows( PnValidationException.class, todo );
	}

	@Test
	void throwsPnInternalExceptionInTheUncommonCaseOfDuplicatedIun() throws IdConflictException {
		// Given
		Mockito.doThrow( new IdConflictException("IUN") )
				.when( notificationDao )
				.addNotification( Mockito.any( Notification.class) );

		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows( PnInternalException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( 1 ) )
				                              .addNotification( Mockito.any( Notification.class ));
	}

	@Test
	void successfullyInsertAfterPartialFailure() throws IdConflictException {
		// Given
		Mockito.doThrow( new PnInternalException("Simulated Error") )
				.doNothing()
				.when( notificationDao )
				.addNotification( Mockito.any( Notification.class) );

		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		Assertions.assertThrows( PnInternalException.class, () ->
				deliveryService.receiveNotification( notification )
			);
		deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao, Mockito.times( 2 ) )
				.addNotification( Mockito.any( Notification.class ));
		Mockito.verify( fileStorage, Mockito.times( 8 ) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );
	}


	private Notification newNotificationWithoutPayments( ) {
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
						NOTIFICATION_ATTACHMENT,
						NOTIFICATION_ATTACHMENT
				))
				.build();
	}

	private Notification newNotificationWithPaymentsDeliveryMode( ) {
		return newNotificationWithoutPayments( ).toBuilder()
				.payment( NotificationPaymentInfo.builder()
						.iuv( "IUV_01" )
						.notificationFeePolicy( NotificationPaymentInfoFeePolicies.DELIVERY_MODE )
						.f24( NotificationPaymentInfo.F24.builder()
								.digital( NOTIFICATION_ATTACHMENT )
								.analog( NOTIFICATION_ATTACHMENT )
								.build()
						)
						.build()
				)
				.build();
	}

	Notification newNotificationWithPaymentsFlat( ) {
		return newNotificationWithoutPayments( ).toBuilder()
				.payment( NotificationPaymentInfo.builder()
						.iuv( "IUV_01" )
						.notificationFeePolicy( NotificationPaymentInfoFeePolicies.FLAT_RATE )
						.f24( NotificationPaymentInfo.F24.builder()
								.flatRate( NOTIFICATION_ATTACHMENT )
								.build()
						)
						.build()
				)
				.build();
	}

}

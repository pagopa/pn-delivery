package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	//public static final NotificationAttachment NOTIFICATION_INLINE_ATTACHMENT = NotificationAttachment.builder()
	//		.body(BASE64_BODY)
	//		.contentType("Content/Type")
	//		.digests(NotificationAttachment.Digests.builder()
	//				.sha256(SHA256_BODY)
	//				.build()
	//		)
	//		.build();
	private static final String VERSION_TOKEN = "VERSION_TOKEN";
	private static final String KEY = "KEY";
	public static final NotificationDocument NOTIFICATION_REFERRED_ATTACHMENT = NotificationDocument.builder()
			.ref( NotificationAttachmentBodyRef.builder()
					.versionToken( VERSION_TOKEN )
					.key( KEY )
					.build() )
			.digests( NotificationAttachmentDigests.builder()
					.sha256(SHA256_BODY)
					.build() )
			.contentType("application/pdf")
			.build();

	private NotificationDao notificationDao;
	private DirectAccessService directAccessService;
	private NewNotificationProducer notificationEventProducer;
	private NotificationReceiverService deliveryService;
	private FileStorage fileStorage;

	@BeforeEach
	public void setup() {
		Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

		notificationDao = Mockito.mock(NotificationDao.class);
		directAccessService = Mockito.mock(DirectAccessService.class);
		notificationEventProducer = Mockito.mock(NewNotificationProducer.class);
		fileStorage = Mockito.mock( FileStorage.class );

		// - Separate Tests
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator() );
		AttachmentService attachmentSaver = new AttachmentService( fileStorage );

		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				directAccessService,
				notificationEventProducer,
				attachmentSaver,
				validator
		    );
	}

	/*@Test
	void successWritingNotificationWithPaymentsInformationWithDeliveryModeFee() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		//NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture() );

		InternalNotification savedNotification = savedNotificationCaptor.getValue();
		//assertEquals( EncodingUtils.base64Encoding(savedNotification.getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notification.getPaNotificationId(), savedNotification.getPaProtocolNumber(), "Wrong protocol number");
		//assertEquals( notification.getPaNotificationId(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

		Mockito.verify( fileStorage, Mockito.times(4) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}*/

	@Test
	void successWritingNotificationWithPaymentsInformationWithFlatFee() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		InternalNotification notification = newNotificationWithPaymentsFlat( );

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals(EncodingUtils.base64Encoding(savedNotification.getValue().getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notification.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notification.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");


		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	@Test
	void successWritingNotificationWithoutPaymentsInformation() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		InternalNotification notification = newNotificationWithoutPayments( );

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( EncodingUtils.base64Encoding(savedNotification.getValue().getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notification.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notification.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	@Test
	void successWritingNotificationWithoutPaymentsAttachment() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		InternalNotification notification = newNotificationWithoutPayments();

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( EncodingUtils.base64Encoding(savedNotification.getValue().getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notification.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notification.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}


	/*@Test
	void throwsPnValidationExceptionForInvalidFormatNotification() {

		// Given
		InternalNotification notification = new InternalNotification(FullSentNotification.builder()
				.recipients( Collections.singletonList( NotificationRecipient.builder().build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder().build() ) )
				.build(), Collections.EMPTY_MAP  );

		// When
		Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		Assertions.assertThrows( PnValidationException.class, todo );
	}*/

	/*@Test
	void throwsPnInternalExceptionInTheUncommonCaseOfDuplicatedIun() throws IdConflictException {
		// Given
		Mockito.doThrow( new IdConflictException("IUN") )
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		//Executable todo = () -> deliveryService.receiveNotification( notification );

		// Then
		//Assertions.assertThrows( PnInternalException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( 1 ) )
				                              .addNotification( Mockito.any( InternalNotification.class ));
	}*/

	/*@Test
	void successfullyInsertAfterPartialFailure() throws IdConflictException {
		// Given
		Mockito.doThrow( new PnInternalException("Simulated Error") )
				.doNothing()
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		Notification notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		//Assertions.assertThrows( PnInternalException.class, () ->
		//		deliveryService.receiveNotification( notification )
		//	);
		//deliveryService.receiveNotification( notification );

		// Then
		Mockito.verify( notificationDao, Mockito.times( 2 ) )
				.addNotification( Mockito.any( InternalNotification.class ));
		Mockito.verify( fileStorage, Mockito.times( 8 ) )
				.putFileVersion( Mockito.anyString(), Mockito.any(InputStream.class), Mockito.anyLong(), Mockito.anyString(), Mockito.anyMap() );
	}*/

	private InternalNotification newNotificationWithoutPayments( ) {
		return new InternalNotification(FullSentNotification.builder()
				.iun("IUN_01")
				.sentAt( Date.from(Instant.now()))
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
				.cancelledByIun("IUN_05")
				.cancelledIun("IUN_00")
				.senderPaId(" pa_02")
				.recipients( Collections.singletonList(
						it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient.builder()
								.recipientType( it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient.RecipientTypeEnum.PF )
								.taxId("Codice Fiscale 01")
								.denomination("Nome Cognome/Ragione Sociale")
								.digitalDomicile(NotificationDigitalAddress.builder()
										.type(NotificationDigitalAddress.TypeEnum.PEC)
										.address("account@dominio.it")
										.build())
								.build()
				))
				.documents(Arrays.asList(
						NOTIFICATION_REFERRED_ATTACHMENT
				))
				.group( "Group_1" )
				.build(), Collections.EMPTY_MAP);
	}

	/*private InternalNotification newNotificationWithoutPaymentsRef( ) {
		return newNotificationWithoutPayments().toBuilder()
				.documents(Arrays.asList(
						NOTIFICATION_REFERRED_ATTACHMENT
				))
				.build();
	}*/

	/*private Notification newNotificationWithPaymentsDeliveryMode( ) {
		return newNotificationWithoutPayments( ).toBuilder()
				.payment( NotificationPaymentInfo.builder()
						.iuv( "IUV_01" )
						.notificationFeePolicy( NotificationPaymentInfoFeePolicies.DELIVERY_MODE )
						.f24( NotificationPaymentInfo.F24.builder()
								.digital(NOTIFICATION_INLINE_ATTACHMENT)
								.analog(NOTIFICATION_INLINE_ATTACHMENT)
								.build()
						)
						.build()
				)
				.build();
	}*/

	private InternalNotification newNotificationWithPaymentsFlat( ) {
		NotificationRecipient recipient = NotificationRecipient.builder()
				.taxId( "Codice Fiscale 02" )
				.physicalAddress( NotificationPhysicalAddress.builder()
						.municipality( "municipality" )
						.zip( "zip_code" )
						.address( "address" )
						.build())
				.denomination( "denomination" )
				.digitalDomicile( NotificationDigitalAddress.builder()
						.type( NotificationDigitalAddress.TypeEnum.PEC )
						.address( "digitalAddressPec" )
						.build() )
				.payment( NotificationPaymentInfo.builder()
						//.iuv( "IUV_01" )
						.notificationFeePolicy( NotificationPaymentInfo.NotificationFeePolicyEnum.FLAT_RATE )
						.f24flatRate( NotificationPaymentAttachment.builder()
								.ref( NotificationAttachmentBodyRef.builder()
										.key( KEY )
										.versionToken( VERSION_TOKEN )
										.build() )
								.digests( NotificationAttachmentDigests.builder()
										.sha256( SHA256_BODY )
										.build() )
								.contentType( "application/pdf" )
								.build()
						)
						.build()
				).build();
		return new InternalNotification( FullSentNotification.builder()
				.iun("IUN_01")
				.sentAt( Date.from(Instant.now()))
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
				.cancelledByIun("IUN_05")
				.cancelledIun("IUN_00")
				.senderPaId(" pa_02")
				.recipients( Collections.singletonList( recipient ) )
				.documents(List.of(
						NOTIFICATION_REFERRED_ATTACHMENT
				))
				.group( "Group_1" )
				.build(), Collections.EMPTY_MAP );
	}

}


package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons_delivery.utils.EncodingUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.Base64Utils;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String VERSION_TOKEN = "VERSION_TOKEN";
	private static final String KEY = "KEY";
	private static final String PAID = "PAID";
	public static final NotificationDocument NOTIFICATION_REFERRED_ATTACHMENT = NotificationDocument.builder()
			.ref( NotificationAttachmentBodyRef.builder()
					.versionToken( VERSION_TOKEN )
					.key( KEY )
					.build() )
			.digests( NotificationAttachmentDigests.builder()
					.sha256(SHA256_BODY)
					.build() )
			.contentType( "application/pdf" )
			.build();

	private NotificationDao notificationDao;
	private DirectAccessService directAccessService;
	private NewNotificationProducer notificationEventProducer;
	private NotificationReceiverService deliveryService;
	private FileStorage fileStorage;
	private ModelMapperFactory modelMapperFactory;

	@BeforeEach
	public void setup() {
		Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

		notificationDao = Mockito.mock(NotificationDao.class);
		directAccessService = Mockito.mock(DirectAccessService.class);
		notificationEventProducer = Mockito.mock(NewNotificationProducer.class);
		fileStorage = Mockito.mock( FileStorage.class );
		modelMapperFactory = Mockito.mock( ModelMapperFactory.class );

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
				validator,
				modelMapperFactory);
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
		//InternalNotification notification = newNotificationWithPaymentsFlat( );
		NewNotificationRequest notificationRequest = newNotificationRequest();

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	//@Test
	void successWritingNotificationWithoutPaymentsInformation() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		//InternalNotification notification = newNotificationWithoutPayments( );
		NewNotificationRequest notificationRequest = newNotificationRequest();

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( EncodingUtils.base64Encoding(savedNotification.getValue().getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notificationRequest.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notificationRequest.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

		Mockito.verify( notificationEventProducer ).sendNewNotificationEvent( Mockito.anyString(), Mockito.anyString(), Mockito.any( Instant.class) );
	}

	//@Test
	void successWritingNotificationWithoutPaymentsAttachment() throws IdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequest newNotificationRequest = newNotificationRequest();

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( EncodingUtils.base64Encoding(savedNotification.getValue().getIun()), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

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

	private NewNotificationRequest newNotificationRequest() {
		return NewNotificationRequest.builder()
				.senderTaxId( "senderTaxId" )
				.senderDenomination( "senderDenomination" )
				.group( "group1" )
				.subject( "subject" )
				.documents( Collections.singletonList( NotificationDocument.builder()
						.contentType( "application/pdf" )
						.digests( NotificationAttachmentDigests.builder()
								.sha256( SHA256_BODY )
								.build() )
						.ref( NotificationAttachmentBodyRef.builder()
								.versionToken( VERSION_TOKEN )
								.key( KEY )
								.build() )
						.build() ) )
				.paProtocolNumber( "paProtocolNumber" )
				.recipients( Collections.singletonList( NotificationRecipient.builder()
						.payment( NotificationPaymentInfo.builder()
								.notificationFeePolicy( NotificationPaymentInfo.NotificationFeePolicyEnum.FLAT_RATE )
								.creditorTaxId( "creditorTaxId" )
								.f24flatRate( NotificationPaymentAttachment.builder()
										.digests( NotificationAttachmentDigests.builder()
												.sha256( SHA256_BODY )
												.build() )
										.contentType( "application/pdf" )
										.ref( NotificationAttachmentBodyRef.builder()
												.key( KEY )
												.versionToken( VERSION_TOKEN )
												.build() )
										.build() )
								.pagoPaForm( NotificationPaymentAttachment.builder()
										.ref(NotificationAttachmentBodyRef.builder()
												.key( KEY )
												.versionToken( VERSION_TOKEN )
												.build())
										.contentType( "application/pdf" )
										.digests( NotificationAttachmentDigests.builder()
												.sha256( SHA256_BODY )
												.build() )
										.build() )
								.build() )
						.recipientType( NotificationRecipient.RecipientTypeEnum.PF )
						.denomination( "recipientDenomination" )
						.taxId( "recipientTaxId" )
						.digitalDomicile( NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address( "address@pec.it" )
								.build() )
						.build() ) )
				.physicalCommunicationType( NewNotificationRequest.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				.build();
	}

	private InternalNotification newNotificationWithoutPayments( ) {
		return new InternalNotification(FullSentNotification.builder()
				.notificationStatus( NotificationStatus.ACCEPTED )
				.iun("IUN_01")
				.sentAt( Date.from(Instant.now()))
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
				.cancelledByIun("IUN_05")
				.cancelledIun("IUN_00")
				.senderPaId(" pa_02")
				.timeline( Collections.singletonList( TimelineElement.builder().build() ) )
				.notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
								.status( NotificationStatus.ACCEPTED )
								.relatedTimelineElements( Collections.emptyList() )
								.activeFrom( Date.from( Instant.now() ) )
						.build() ) )
				.recipients( Collections.singletonList(
						NotificationRecipient.builder()
								.recipientType( NotificationRecipient.RecipientTypeEnum.PF )
								.taxId("Codice Fiscale 01")
								.denomination("Nome Cognome/Ragione Sociale")
								.digitalDomicile(NotificationDigitalAddress.builder()
										.type(NotificationDigitalAddress.TypeEnum.PEC)
										.address("account@dominio.it")
										.build())
								.build()
				))
				.documents(List.of(
						NOTIFICATION_REFERRED_ATTACHMENT
				))
				.group( "Group_1" )
				.build(), Collections.emptyMap());
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
				.recipientType( NotificationRecipient.RecipientTypeEnum.PF )
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
						.creditorTaxId( "creditorTaxId" )
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
						.pagoPaForm( NotificationPaymentAttachment.builder()
								.ref( NotificationAttachmentBodyRef.builder()
										.key( KEY )
										.versionToken( VERSION_TOKEN )
										.build() )
								.digests( NotificationAttachmentDigests.builder()
										.sha256( SHA256_BODY )
										.build() )
								.contentType( "application/pdf" )
								.build() )
						.build()
				).build();
		return new InternalNotification( FullSentNotification.builder()
				.iun("IUN_01")
				.notificationStatus( NotificationStatus.ACCEPTED )
				.notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
								.activeFrom( Date.from( Instant.now() ) )
								.relatedTimelineElements( Collections.emptyList() )
								.status( NotificationStatus.ACCEPTED )
						.build() ) )
				.timeline( Collections.singletonList( TimelineElement.builder()
						.build() ) )
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


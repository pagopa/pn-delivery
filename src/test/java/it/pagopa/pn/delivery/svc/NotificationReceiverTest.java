package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import it.pagopa.pn.delivery.utils.NotificationDaoMock;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.util.Base64Utils;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String BASE64_BODY = Base64Utils.encodeToString(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8));
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String VERSION_TOKEN = "VERSION_TOKEN";
	private static final String CONTENT_TYPE = "application/pdf";
	private static final String KEY = "KEY";
	private static final String PAID = "PAID";
	private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
	private static final NotificationDocument notificationReferredAttachment() {
		return NotificationDocument.builder()
				.ref( NotificationAttachmentBodyRef.builder()
						.versionToken( VERSION_TOKEN )
						.key( KEY )
						.build() )
				.digests( NotificationAttachmentDigests.builder()
						.sha256(SHA256_BODY)
						.build() )
				.contentType( CONTENT_TYPE )
				.build();
	}
	public static final String X_PAGOPA_PN_CX_ID = "paId";

	private NotificationDao notificationDao;
	private NotificationReceiverService deliveryService;
	private FileStorage fileStorage;
	private ModelMapperFactory modelMapperFactory;
	private MVPParameterConsumer mvpParameterConsumer;

	@BeforeEach
	public void setup() {
		Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

		notificationDao = Mockito.spy( new NotificationDaoMock() );
		fileStorage = Mockito.mock( FileStorage.class );
		modelMapperFactory = Mockito.mock( ModelMapperFactory.class );
		mvpParameterConsumer = Mockito.mock( MVPParameterConsumer.class );

		// - Separate Tests
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator(), mvpParameterConsumer);

		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				validator,
				modelMapperFactory);
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithDeliveryModeFee() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		NewNotificationRequest notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification(X_PAGOPA_PN_CX_ID, notification );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture()  );

		InternalNotification savedNotification = savedNotificationCaptor.getValue();

		assertEquals( notification.getPaProtocolNumber(), savedNotification.getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notification.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithFlatFee() throws PnIdConflictException {
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
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture()  );
	}

	@Test
	void successWritingNotificationWithoutPaymentsInformation() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		//InternalNotification notification = newNotificationWithoutPayments( );
		NewNotificationRequest notificationRequest = newNotificationRequest();

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( Base64Utils.encodeToString(savedNotification.getValue().getIun().getBytes(StandardCharsets.UTF_8)), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( notificationRequest.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notificationRequest.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

		assertEquals( notificationRequest.getAbstract(), savedNotification.getValue().getAbstract() );

	}

	@Test
	void successWritingNotificationWithoutPaymentsAttachment() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequest newNotificationRequest = newNotificationRequest();

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest );

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( Base64Utils.encodeToString(savedNotification.getValue().getIun().getBytes(StandardCharsets.UTF_8)), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

	}


	@Test
	void throwsPnValidationExceptionForInvalidFormatNotification() {

		// Given
		NewNotificationRequest notification = NewNotificationRequest.builder()
				.recipients( Collections.singletonList( NotificationRecipient.builder().build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder().build() ) )
				.build();

		// When
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification );

		// Then
		Assertions.assertThrows( PnValidationException.class, todo );
	}

	@Test
	void throwsPnValidationExceptionForInvalidFormatNotificationForMVP() {

		// Given
		NewNotificationRequest notification = newNotificationRequest();
		notification.setSenderDenomination( null );

		// When
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification );

		// Then
		Assertions.assertThrows( PnValidationException.class, todo );
	}

	@Test
	void throwsPnInternalExceptionInTheUncommonCaseOfDuplicatedIun() throws PnIdConflictException {
		// Given
		Map<String,String> keyValueConflict = new HashMap<>();
		keyValueConflict.put( "iun", IUN );
		Mockito.doThrow( new PnIdConflictException(keyValueConflict) )
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		NewNotificationRequest notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.contentType( CONTENT_TYPE )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification );

		// Then
		PnIdConflictException exc = Assertions.assertThrows( PnIdConflictException.class, todo );
		Mockito.verify( notificationDao, Mockito.times( 1 ) )
				                              .addNotification( Mockito.any( InternalNotification.class ));
	}

	@Test
	void successfullyInsertAfterPartialFailure() throws PnIdConflictException {
		// Given
		Mockito.doThrow( new PnInternalException("Simulated Error") )
				.doNothing()
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		NewNotificationRequest notification = newNotificationWithPaymentsDeliveryMode( );

		// When
		ModelMapper mapper = new ModelMapper();
		mapper.createTypeMap( NewNotificationRequest.class, InternalNotification.class );
		Mockito.when( modelMapperFactory.createModelMapper( NewNotificationRequest.class, InternalNotification.class ) ).thenReturn( mapper );

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.contentType( CONTENT_TYPE )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		Assertions.assertThrows( PnInternalException.class, () ->
				deliveryService.receiveNotification( "paId", notification )
		);

		deliveryService.receiveNotification( "paId", notification );

		// Then
		Mockito.verify( notificationDao, Mockito.times( 2 ) )
				.addNotification( Mockito.any( InternalNotification.class ));
	}

	private NewNotificationRequest newNotificationRequest() {
		return NewNotificationRequest.builder()
				.senderTaxId( "01199250158" )
				.senderDenomination( "Comune di Milano" )
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
				.notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.FLAT_RATE )
				.paProtocolNumber( "paProtocolNumber" )
				.recipients( Collections.singletonList( NotificationRecipient.builder()
						.payment( NotificationPaymentInfo.builder()
								.creditorTaxId( "77777777777" )
								.noticeCode("123456789012345678")
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
						.denomination( "Ada Lovelace" )
						.taxId( "LVLDAA85T50G702B" )
						.digitalDomicile( NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address( "address@pec.it" )
								.build() )
						.physicalAddress( NotificationPhysicalAddress.builder()
								.at( "at" )
								.province( "province" )
								.zip( "00100" )
								.address( "address" )
								.addressDetails( "addressDetail" )
								.municipality( "municipality" )
								.municipalityDetails( "municipalityDetail" )
								.build() )
						.build() ))
				.physicalCommunicationType( NewNotificationRequest.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				._abstract( "abstract" )
				.build();
	}

	private NewNotificationRequest newNotificationWithoutPayments( ) {
		return NewNotificationRequest.builder()
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( NewNotificationRequest.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
				.cancelledIun(IUN)
				.recipients( Collections.singletonList(
						NotificationRecipient.builder()
								.recipientType( NotificationRecipient.RecipientTypeEnum.PF )
								.taxId("LVLDAA85T50G702B")
								.denomination("Ada Lovelace")
								.digitalDomicile(NotificationDigitalAddress.builder()
										.type(NotificationDigitalAddress.TypeEnum.PEC)
										.address("account@dominio.it")
										.build())
								.build()
				))
				.documents(List.of(
						notificationReferredAttachment()
				))
				.group( "Group_1" )
				.senderTaxId( "01199250158" )
				.senderDenomination( "Comune di Milano" )
				.build();
	}

	private NewNotificationRequest newNotificationWithPaymentsDeliveryMode( ) {
		NewNotificationRequest notification = newNotificationWithoutPayments( );
		notification.notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.DELIVERY_MODE );

		for( NotificationRecipient recipient : notification.getRecipients()) {
			recipient.payment( NotificationPaymentInfo.builder()
					.noticeCode( "123456789012345678" )
					.f24flatRate( buildPaymentAttachment() )
					.f24standard( buildPaymentAttachment() )
					.pagoPaForm( buildPaymentAttachment()  )
					.creditorTaxId( "12345678901" )
					.build()
			);
		}
		return notification;
	}

	private NewNotificationRequest newNotificationWithPaymentsFlat( ) {
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
		return NewNotificationRequest.builder()
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( NewNotificationRequest.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
				.cancelledIun(IUN)
				.recipients( Collections.singletonList( recipient ) )
				.documents(List.of(
						notificationReferredAttachment()
				))
				.group( "Group_1" )
				.build();
	}

	private NotificationPaymentAttachment buildPaymentAttachment() {
		return NotificationPaymentAttachment.builder()
				.ref( NotificationAttachmentBodyRef.builder().key("k1").versionToken("v1").build())
				.contentType("application/pdf")
				.digests( NotificationAttachmentDigests.builder().sha256("sha256").build())
				.build();
	}

}


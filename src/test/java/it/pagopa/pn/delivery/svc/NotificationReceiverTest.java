package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.SendActiveParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroupStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.pnclient.pnf24.PnF24ClientImpl;
import it.pagopa.pn.delivery.utils.NotificationDaoMock;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";
	private static final String VERSION_TOKEN = "VERSION_TOKEN";
	private static final String CONTENT_TYPE = "application/pdf";
	private static final String KEY = "KEY";
	private static final String PAID = "PAID";
	private static final String IUN = "FAKE-FAKE-FAKE-202209-F-1";
	private static NotificationDocument notificationReferredAttachment() {
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

	public static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
	public static final List<String> X_PAGOPA_PN_CX_GROUPS_EMPTY = Collections.emptyList();
	public static final List<String> X_PAGOPA_PN_CX_GROUPS = List.of( "group1" );

	private NotificationDao notificationDao;
	private NotificationReceiverService deliveryService;
	private FileStorage fileStorage;
	private ModelMapper modelMapper;
	private MVPParameterConsumer mvpParameterConsumer;
	private SendActiveParameterConsumer sendActiveParameterConsumer;
	private ValidateUtils validateUtils;
	private PnExternalRegistriesClientImpl pnExternalRegistriesClient;
	private PnDeliveryConfigs pnDeliveryConfigs;
	private PnF24ClientImpl pnF24Client;
	private PnDeliveryConfigs cfg;

	@BeforeEach
	public void setup() {
		Clock clock = Clock.fixed( Instant.EPOCH, ZoneId.of("UTC"));

		notificationDao = Mockito.spy( new NotificationDaoMock() );
		fileStorage = Mockito.mock( FileStorage.class );
		modelMapper = new ModelMapper();
		sendActiveParameterConsumer = Mockito.mock( SendActiveParameterConsumer.class );
		mvpParameterConsumer = Mockito.mock( MVPParameterConsumer.class );
		pnExternalRegistriesClient = Mockito.mock( PnExternalRegistriesClientImpl.class );
		validateUtils = Mockito.mock( ValidateUtils.class );
		pnDeliveryConfigs = Mockito.mock(PnDeliveryConfigs.class);
		pnF24Client = Mockito.mock(PnF24ClientImpl.class);
		cfg = Mockito.mock(PnDeliveryConfigs.class);

		// - Separate Tests
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator(), mvpParameterConsumer, validateUtils, pnDeliveryConfigs);

		Mockito.when( validateUtils.validate( Mockito.anyString() ) ).thenReturn( true );
		Mockito.when( sendActiveParameterConsumer.isSendActive( Mockito.anyString() ) ).thenReturn( true );

		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				validator,
				modelMapper,
				sendActiveParameterConsumer,
				pnExternalRegistriesClient,
				pnF24Client,
				cfg);
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithDeliveryModeFee() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		NewNotificationRequestV21 notification = newNotificationWithPaymentsDeliveryMode( );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true) ) )
				.thenReturn( List.of(new PaGroup().id("Group_1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification(X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

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
		NewNotificationRequestV21 notificationRequest = newNotificationRequest();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture()  );
	}

	@Test
	void successWritingNotificationWithoutPaymentsInformation() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		//InternalNotification notification = newNotificationWithoutPayments( );
		NewNotificationRequestV21 notificationRequest = newNotificationRequest();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

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

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );
		assertEquals( Base64Utils.encodeToString(savedNotification.getValue().getIun().getBytes(StandardCharsets.UTF_8)), addedNotification.getNotificationRequestId(), "Saved iun differ from returned one");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), savedNotification.getValue().getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( newNotificationRequest.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

	}


	@Test
	void successWritingNotificationWithTooMuchAttachments() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(2);

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setDocuments(List.of(NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build(),
				NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build(),
				NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build()));

		// When
		Executable todo = () -> {
			NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);
		};

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );

		// Then
		Mockito.verify( notificationDao, Mockito.never() ).addNotification( savedNotification.capture() );

	}


	@Test
	void successWritingNotificationWithOkAttachments()  {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(5);

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setDocuments(List.of(NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build(),
				NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build(),
				NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder().key("k1"+ UUID.randomUUID()).versionToken("v1").build())
						.contentType("application/pdf")
						.digests( NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
						.build()));

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );

	}


	@Test
	void successWritingNotificationWithTooMuchRecipients() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxRecipientsCount()).thenReturn(2);

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setRecipients(
				List.of(
						buildRecipient("LVLDAA85T50G702B", "888888888888888888"),
						buildRecipient("DSRDNI00A01A225I", "777777777777777777"),
						buildRecipient("GLLGLL64B15G702I", "999999999999999999"))
		);

		// When
		Executable todo = () -> {
			NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);
		};

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );

		// Then
		Mockito.verify( notificationDao, Mockito.never() ).addNotification( savedNotification.capture() );

	}


	@Test
	void successWritingNotificationWithOkRecipients()  {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		// Given
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(3);

		//InternalNotification notification = newNotificationWithoutPayments();
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setRecipients(
				List.of(
						buildRecipient("LVLDAA85T50G702B", "888888888888888888"),
						buildRecipient("DSRDNI00A01A225I", "777777777777777777"),
						buildRecipient("GLLGLL64B15G702I", "999999999999999999"))
		);

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );

	}

	@Test
	void successWriteNotificationWithGroupCheck() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup("group1");

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void successWriteNotificationWithGroupCheckNoGroup() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup(null);

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY );

		// Then
		Assertions.assertNotNull( response );
	}


	@Test
	void successNewNotificationGroupCheckNoNotificationGroupNoSelfCareGroups() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( null );

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void badRequestNewNotificationForSendDisabled() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();

		// When
		Mockito.when( sendActiveParameterConsumer.isSendActive( Mockito.anyString() ) ).thenReturn( false );
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, List.of( "fake_Group" ) );

		// Then
		Assertions.assertThrows(PnBadRequestException.class, todo);
	}

	@Test
	void failureNewNotificationCauseGroupCheck() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();

		// When
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, List.of( "fake_Group" ) );

		// Then
		Assertions.assertThrows(PnInvalidInputException.class, todo);
	}

	@Test
	void failureNewNotificationCauseGroupCheckNoNotificationGroupInHeaderGroups() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( null );

		// When
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS );

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}

	@Test
	void failureNewNotificationCauseGroupCheckNotificationGroupButSelfCareGroupsSuspendend() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( "group_1" );

		// When
		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( new ArrayList<>());

		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY );

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}


	@Test
	void throwsPnValidationExceptionForInvalidFormatNotification() {

		// Given
		NewNotificationRequestV21 notification = NewNotificationRequestV21.builder()
				.senderTaxId( "fakeSenderTaxId" )
				.paProtocolNumber("test")
				.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
				.recipients( Collections.singletonList( NotificationRecipientV21.builder().build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder().build() ) )
				.build();

		// When
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}

	@Test
	void throwsPnValidationExceptionForInvalidFormatNotificationForMVP() {

		// Given
		NewNotificationRequestV21 notification = newNotificationRequest();
		notification.setSenderDenomination( null );

		// When
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

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

		NewNotificationRequestV21 notification = newNotificationWithPaymentsDeliveryMode( );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("Group_1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.contentType( CONTENT_TYPE )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

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

		NewNotificationRequestV21 notification = newNotificationWithPaymentsDeliveryMode( );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("Group_1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.contentType( CONTENT_TYPE )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		Assertions.assertThrows( PnInternalException.class, () ->
				deliveryService.receiveNotification( "paId", notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY)
		);

		deliveryService.receiveNotification( "paId", notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY);

		// Then
		Mockito.verify( notificationDao, Mockito.times( 2 ) )
				.addNotification( Mockito.any( InternalNotification.class ));
	}

	@Test
	void successNewNotificationWithPagoPaIntMode() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup(null);
		newNotificationRequest.setPagoPaIntMode( NewNotificationRequestV21.PagoPaIntModeEnum.SYNC );

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void successNewNotificationNoPagoPaIntModeNoPayment() {
		// Given
		NewNotificationRequestV21 newNotificationRequest = newNotificationWithoutPayments();
		newNotificationRequest.setGroup(null);
		newNotificationRequest.setNotificationFeePolicy( NotificationFeePolicy.DELIVERY_MODE );
		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY );

		// Then
		Assertions.assertNotNull( response );
	}

	private NewNotificationRequestV21 newNotificationRequest() {
		return NewNotificationRequestV21.builder()
				.senderTaxId( "01199250158" )
				.senderDenomination( "Comune di Milano" )
				.group( "group1" )
				.subject( "subject_length" )
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
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.paProtocolNumber( "paProtocolNumber" )
				.recipients( Collections.singletonList(
						buildRecipient("LVLDAA85T50G702B", "888888888888888888") ))
				.physicalCommunicationType( NewNotificationRequestV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				._abstract( "abstract" )
				.build();
	}

	private NewNotificationRequestV21 newNotificationWithoutPayments( ) {
		return NewNotificationRequestV21.builder()
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( NewNotificationRequestV21.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
				.cancelledIun(IUN)
				.recipients( Collections.singletonList(
						NotificationRecipientV21.builder()
								.recipientType( NotificationRecipientV21.RecipientTypeEnum.PF )
								.taxId("LVLDAA85T50G702B")
								.denomination("Ada Lovelace")
								.digitalDomicile(NotificationDigitalAddress.builder()
										.type(NotificationDigitalAddress.TypeEnum.PEC)
										.address("account@dominio.it")
										.build())
								.physicalAddress( NotificationPhysicalAddress.builder()
										.address( "address" )
										.at( "presso" )
										.zip( "83100" )
										.municipality( "municipality" )
										.province( "province" )
										.build()
								)
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

	private NewNotificationRequestV21 newNotificationWithPaymentsDeliveryMode( ) {
		NewNotificationRequestV21 notification = newNotificationWithoutPayments( );
		notification.notificationFeePolicy( NotificationFeePolicy.DELIVERY_MODE );

		for( NotificationRecipientV21 recipient : notification.getRecipients()) {
			recipient.payments( List.of(NotificationPaymentItem.builder()
					.pagoPa(PagoPaPayment.builder()
							.creditorTaxId("00000000000")
							.applyCost(true)
							.noticeCode("000000000000000000")
							.build())
					.f24(F24Payment.builder()
							.title("title")
							.applyCost(true)
							.metadataAttachment(NotificationMetadataAttachment.builder()
									.ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
									.digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
									.contentType("application/json")
									.build())
							.build())
					.build()));
		}
		return notification;
	}

	private NotificationRecipientV21 buildRecipient(String taxID, String noticeCode){
		return NotificationRecipientV21.builder()
				.payments( List.of(NotificationPaymentItem.builder()
						.pagoPa(PagoPaPayment.builder()
								.creditorTaxId("00000000000")
								.applyCost(false)
								.noticeCode(noticeCode)
								.build())
						.f24(F24Payment.builder()
								.title("title")
								.applyCost(false)
								.metadataAttachment(NotificationMetadataAttachment.builder()
										.ref(NotificationAttachmentBodyRef.builder().versionToken(VERSION_TOKEN).key(KEY).build())
										.digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY).build())
										.contentType("application/json")
										.build())
								.build())
						.build()))
				.recipientType( NotificationRecipientV21.RecipientTypeEnum.PF )
				.denomination( "Ada Lovelace" )
				.taxId( taxID )
				.digitalDomicile( NotificationDigitalAddress.builder()
						.type( NotificationDigitalAddress.TypeEnum.PEC )
						.address( "address@pec.it" )
						.build() )
				.physicalAddress( NotificationPhysicalAddress.builder()
						.at( "at" )
						.province( "province" )
						.zip( "83100" )
						.address( "address" )
						.addressDetails( "addressDetail" )
						.municipality( "municipality" )
						.municipalityDetails( "municipalityDetail" )
						.build() )
				.build();
	}


}


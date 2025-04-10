package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.config.SendActiveParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroupStatus;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.api.AgenziaEntrateApi;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

import static it.pagopa.pn.delivery.svc.NotificationReceiverService.PA_FEE_DEFAULT_VALUE;
import static it.pagopa.pn.delivery.svc.NotificationReceiverService.VAT_DEFAULT_VALUE;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationReceiverTest {

	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";
	public static final String SHA256_BODY2 = "X8Q//3XIf4MXiE6LsTYQpkqj4xaPNHXhNkW/iqo1UCo=";
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
	private AgenziaEntrateApi agenziaEntrateApi;
	private PaNotificationLimitService paNotificationLimitService;
	private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter;

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
		agenziaEntrateApi = Mockito.mock(AgenziaEntrateApi.class);
		paNotificationLimitService = Mockito.mock(PaNotificationLimitService.class);
		physicalAddressLookupParameter = Mockito.mock(PhysicalAddressLookupParameterConsumer.class);

		// - Separate Tests
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		NotificationReceiverValidator validator = new NotificationReceiverValidator( factory.getValidator(), mvpParameterConsumer, validateUtils, pnDeliveryConfigs, agenziaEntrateApi, physicalAddressLookupParameter);

		Mockito.when( validateUtils.validate( Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean() ) ).thenReturn( true );
		Mockito.when( sendActiveParameterConsumer.isSendActive( Mockito.anyString() ) ).thenReturn( true );

		deliveryService = new NotificationReceiverService(
				clock,
				notificationDao,
				validator,
				modelMapper,
				sendActiveParameterConsumer,
				pnExternalRegistriesClient,
				pnF24Client,
				cfg,
				paNotificationLimitService);
	}

	private void defaultMockConfigAndParameterForVas(){
		Mockito.when(physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup()).thenReturn(List.of("01199250158"));
		Mockito.when(pnDeliveryConfigs.getPhysicalAddressLookupStartDate()).thenReturn(Instant.now().minus(5, ChronoUnit.MINUTES));
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithDeliveryModeFee() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		NewNotificationRequestV25 notification = newNotificationWithPaymentsDeliveryMode( );

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true) ) )
				.thenReturn( List.of(new PaGroup().id("Group_1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		defaultMockConfigAndParameterForVas();
		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		NewNotificationResponse addedNotification = deliveryService.receiveNotification(X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

		// Then
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture()  );

		InternalNotification savedNotification = savedNotificationCaptor.getValue();

		assertEquals( notification.getPaProtocolNumber(), savedNotification.getPaProtocolNumber(), "Wrong protocol number");
		assertEquals( notification.getPaProtocolNumber(), addedNotification.getPaProtocolNumber(), "Wrong protocol number");

	}

	@Test
	void checkVatPaFeeDefaultValue() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup("group1");
		newNotificationRequest.setVat(null);
		newNotificationRequest.setPaFee(null);

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		defaultMockConfigAndParameterForVas();

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null );

		// Then
		Assertions.assertNotNull( response );
		
		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture()  );

		InternalNotification savedNotification = savedNotificationCaptor.getValue();

		assertEquals( VAT_DEFAULT_VALUE, savedNotification.getVat() );
		assertEquals( PA_FEE_DEFAULT_VALUE, savedNotification.getPaFee());
	}

	@Test
	void checkVatPaFeeWithoutDefaultValue() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup("group1");
		newNotificationRequest.setVat(10);
		newNotificationRequest.setPaFee(80);

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		defaultMockConfigAndParameterForVas();

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null );

		// Then
		Assertions.assertNotNull( response );

		ArgumentCaptor<InternalNotification> savedNotificationCaptor = ArgumentCaptor.forClass(InternalNotification.class);
		Mockito.verify( notificationDao ).addNotification( savedNotificationCaptor.capture()  );

		InternalNotification savedNotification = savedNotificationCaptor.getValue();

		assertEquals( newNotificationRequest.getVat(), savedNotification.getVat() );
		assertEquals( newNotificationRequest.getPaFee(), savedNotification.getPaFee());
	}

	@Test
	void successWritingNotificationWithPaymentsInformationWithFlatFee() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		NewNotificationRequestV25 notificationRequest = newNotificationRequest();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		defaultMockConfigAndParameterForVas();

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

        deliveryService.receiveNotification(PAID, notificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

        // Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture()  );
	}

	@Test
	void successWritingNotificationWithoutPaymentsInformation() throws PnIdConflictException {
		ArgumentCaptor<InternalNotification> savedNotification = ArgumentCaptor.forClass(InternalNotification.class);

		// Given
		NewNotificationRequestV25 notificationRequest = newNotificationRequest();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		defaultMockConfigAndParameterForVas();

		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,notificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

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

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();

		// When
		NewNotificationResponse addedNotification = deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

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

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(2);

		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
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
			deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);
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

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(5);

		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
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
        deliveryService.receiveNotification(PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

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

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxRecipientsCount()).thenReturn(2);

		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setRecipients(
				List.of(
						buildRecipient("LVLDAA85T50G702B", "888888888888888888"),
						buildRecipient("DSRDNI00A01A225I", "777777777777777777"),
						buildRecipient("GLLGLL64B15G702I", "999999999999999999"))
		);

		// When
		Executable todo = () -> {
			deliveryService.receiveNotification( PAID ,newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);
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

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));


		Mockito.when( pnDeliveryConfigs.getMaxAttachmentsCount()).thenReturn(3);

		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setRecipients(
				List.of(
						buildRecipient("LVLDAA85T50G702B", "888888888888888888"),
						buildRecipient("DSRDNI00A01A225I", "777777777777777777"),
						buildRecipient("GLLGLL64B15G702I", "999999999999999999"))
		);

		// When
        deliveryService.receiveNotification(PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

        // Then
		Mockito.verify( notificationDao ).addNotification( savedNotification.capture() );

	}

	@Test
	void successWriteNotificationWithGroupCheck() {
		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup("group1");

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void successWriteNotificationWithGroupCheckNoGroup() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup(null);

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));

		defaultMockConfigAndParameterForVas();

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null );

		// Then
		Assertions.assertNotNull( response );
	}


	@Test
	void successNewNotificationGroupCheckNoNotificationGroupNoSelfCareGroups() {
		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( null );

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void badRequestNewNotificationForSendDisabled() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();

		defaultMockConfigAndParameterForVas();

		// When
		Mockito.when( sendActiveParameterConsumer.isSendActive( Mockito.anyString() ) ).thenReturn( false );
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, List.of( "fake_Group" ), null );

		// Then
		Assertions.assertThrows(PnBadRequestException.class, todo);
	}

	@Test
	void failureNewNotificationCauseGroupCheck() {
		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();

		// When
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, List.of( "fake_Group" ), null );

		// Then
		Assertions.assertThrows(PnInvalidInputException.class, todo);
	}

	@Test
	void failureNewNotificationCauseGroupCheckNoNotificationGroupInHeaderGroups() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( null );

		defaultMockConfigAndParameterForVas();

		// When
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null );

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}

	@Test
	void failureNewNotificationCauseGroupCheckNotificationGroupButSelfCareGroupsSuspendend() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup( "group_1" );

		defaultMockConfigAndParameterForVas();

		// When
		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( new ArrayList<>());

		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null );

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}


	@Test
	void throwsPnValidationExceptionForInvalidFormatNotification() {

		// Given
		NewNotificationRequestV25 notification = NewNotificationRequestV25.builder()
				.senderTaxId( "fakeSenderTaxId" )
				.paProtocolNumber("test")
				.notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
				.recipients( Collections.singletonList( NotificationRecipientV24.builder()
								.physicalAddress(NotificationPhysicalAddress.builder().build())
								.recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
						.build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder().build() ) )
				.build();

		defaultMockConfigAndParameterForVas();

		// When
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}

	@Test
	void throwsPnValidationExceptionForInvalidFormatNotificationForMVP() {

		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 notification = newNotificationRequest();
		notification.setSenderDenomination( null );

		// When
		Mockito.when( mvpParameterConsumer.isMvp( Mockito.anyString() ) ).thenReturn( false );
		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

		// Then
		Assertions.assertThrows( PnValidationException.class, todo );
	}

	@Test
	void throwsPnInternalExceptionInTheUncommonCaseOfDuplicatedIun() throws PnIdConflictException {
		// Given
		Map<String,String> keyValueConflict = new HashMap<>();
		keyValueConflict.put( "iun", IUN );
		doThrow( new PnIdConflictException(keyValueConflict) )
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		NewNotificationRequestV25 notification = newNotificationWithPaymentsDeliveryMode( );

		defaultMockConfigAndParameterForVas();

		Mockito.when( pnExternalRegistriesClient.getGroups( Mockito.anyString(), Mockito.eq(true ) ) )
				.thenReturn( List.of(new PaGroup().id("Group_1").status(PaGroupStatus.ACTIVE)));

		// When
		FileData fileData = FileData.builder()
				.content( new ByteArrayInputStream(ATTACHMENT_BODY_STR.getBytes(StandardCharsets.UTF_8)) )
				.contentType( CONTENT_TYPE )
				.build();

		Mockito.when( fileStorage.getFileVersion( Mockito.anyString(), Mockito.anyString()))
				.thenReturn( fileData );

		Executable todo = () -> deliveryService.receiveNotification( X_PAGOPA_PN_CX_ID, notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

		// Then
        Assertions.assertThrows(PnIdConflictException.class, todo);
        Mockito.verify( notificationDao, Mockito.times( 1 ) )
				.addNotification( Mockito.any( InternalNotification.class ));
	}

	@Test
	void successfullyInsertAfterPartialFailure() throws PnIdConflictException {
		// Given
		doThrow( new PnInternalException("Simulated Error") )
				.doNothing()
				.when( notificationDao )
				.addNotification( Mockito.any( InternalNotification.class) );

		NewNotificationRequestV25 notification = newNotificationWithPaymentsDeliveryMode( );

		defaultMockConfigAndParameterForVas();

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
				deliveryService.receiveNotification( "paId", notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null)
		);

		deliveryService.receiveNotification( "paId", notification, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null);

		// Then
		Mockito.verify( notificationDao, Mockito.times( 2 ) )
				.addNotification( Mockito.any( InternalNotification.class ));
	}

	@Test
	void successNewNotificationWithPagoPaIntMode() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setGroup(null);
		newNotificationRequest.setPagoPaIntMode( NewNotificationRequestV25.PagoPaIntModeEnum.SYNC );

		defaultMockConfigAndParameterForVas();

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void successNewNotificationNoPagoPaIntModeNoPayment() {
		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationWithoutPayments();
		newNotificationRequest.setGroup(null);
		newNotificationRequest.setNotificationFeePolicy( NotificationFeePolicy.FLAT_RATE );

		defaultMockConfigAndParameterForVas();

		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, null );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void successNewNotificationNoPagoPaIntModeNoPaymentWithNotificationVersion() {
		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationWithoutPayments();
		newNotificationRequest.setGroup(null);
		newNotificationRequest.setNotificationFeePolicy( NotificationFeePolicy.FLAT_RATE );
		// When
		NewNotificationResponse response = deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS_EMPTY, "1" );

		// Then
		Assertions.assertNotNull( response );
	}

	@Test
	void throwsPnValidationExceptionForMissingTaxonomyCode() {
		defaultMockConfigAndParameterForVas();

		// Given
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.setTaxonomyCode( null );


		// When
		Executable todo = () -> deliveryService.receiveNotification( PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null );

		// Then
		Assertions.assertThrows( PnInvalidInputException.class, todo );
	}

	@Test
	void receiveNotification_checkIfPaNotificationLimitExistsTrue() {
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		when(pnExternalRegistriesClient.getGroups(Mockito.anyString(), Mockito.eq(true)))
				.thenReturn(List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		when(paNotificationLimitService.checkIfPaNotificationLimitExists(any(InternalNotification.class))).thenReturn(true);

		defaultMockConfigAndParameterForVas();

		NewNotificationResponse response = deliveryService.receiveNotification(PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null);

		assertNotNull(response);
		verify(paNotificationLimitService).checkIfPaNotificationLimitExists(any(InternalNotification.class));
		verify(paNotificationLimitService).decrementLimitIncrementDailyCounter(any(InternalNotification.class));
	}

	@Test
	void receiveNotification_checkIfPaNotificationLimitExistsTrueAndPnIdConflictException() {
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		when(pnExternalRegistriesClient.getGroups(Mockito.anyString(), Mockito.eq(true)))
				.thenReturn(List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		when(paNotificationLimitService.checkIfPaNotificationLimitExists(any(InternalNotification.class))).thenReturn(true);
		doThrow(new PnIdConflictException(new HashMap<>())).when(notificationDao).addNotification(any(InternalNotification.class));

		defaultMockConfigAndParameterForVas();


		Executable todo = () -> deliveryService.receiveNotification(PAID, newNotificationRequest, X_PAGOPA_PN_SRC_CH, null, X_PAGOPA_PN_CX_GROUPS, null);

		Assertions.assertThrows(PnIdConflictException.class, todo);
		verify(paNotificationLimitService).checkIfPaNotificationLimitExists(any(InternalNotification.class));
		verify(paNotificationLimitService).decrementLimitIncrementDailyCounter(any(InternalNotification.class));
		verify(paNotificationLimitService).incrementLimitDecrementDailyCounter(any(InternalNotification.class));
	}

	@Test
	void receiveNotification_setPhysicalAddressLookup() {
		NewNotificationRequestV25 newNotificationRequest = newNotificationRequest();
		newNotificationRequest.getRecipients().get(0).setPhysicalAddress(null);

		when(pnExternalRegistriesClient.getGroups(Mockito.anyString(), Mockito.eq(true)))
				.thenReturn(List.of(new PaGroup().id("group1").status(PaGroupStatus.ACTIVE)));
		when(paNotificationLimitService.checkIfPaNotificationLimitExists(any(InternalNotification.class))).thenReturn(false);

		defaultMockConfigAndParameterForVas();

		NewNotificationResponse response = deliveryService.receiveNotification(
				PAID,
				newNotificationRequest,
				X_PAGOPA_PN_SRC_CH,
				null,
				X_PAGOPA_PN_CX_GROUPS,
				null
		);

		assertNotNull(response);
		ArgumentCaptor<InternalNotification> captor = ArgumentCaptor.forClass(InternalNotification.class);
		verify(notificationDao).addNotification(captor.capture());
		InternalNotification capturedNotification = captor.getValue();
		assertNotNull(capturedNotification.getUsedServices());
		assertTrue(capturedNotification.getUsedServices().getPhysicalAddressLookup());
	}

	private NewNotificationRequestV25 newNotificationRequest() {
		return NewNotificationRequestV25.builder()
				.senderTaxId( "01199250158" )
				.senderDenomination( "Comune di Milano" )
				.group( "group1" )
				.subject( "subject_length" )
				.taxonomyCode("010101P")
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
				.physicalCommunicationType( NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				._abstract( "abstract" )
				.build();
	}

	private NewNotificationRequestV25 newNotificationWithoutPayments( ) {
		return NewNotificationRequestV25.builder()
				.paProtocolNumber("protocol_01")
				.subject("Subject 01")
				.physicalCommunicationType( NewNotificationRequestV25.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
				.cancelledIun(IUN)
				.recipients( Collections.singletonList(
						NotificationRecipientV24.builder()
								.recipientType( NotificationRecipientV24.RecipientTypeEnum.PF )
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
				.taxonomyCode("010101P")
				.build();
	}

	private NewNotificationRequestV25 newNotificationWithPaymentsDeliveryMode( ) {
		NewNotificationRequestV25 notification = newNotificationWithoutPayments( );
		notification.notificationFeePolicy( NotificationFeePolicy.DELIVERY_MODE );
		notification.setPaFee(100);
		notification.setVat(22);

		for( NotificationRecipientV24 recipient : notification.getRecipients()) {
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
									.digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY2).build())
									.contentType("application/json")
									.build())
							.build())
					.build()));
		}
		return notification;
	}

	private NotificationRecipientV24 buildRecipient(String taxID, String noticeCode){
		return NotificationRecipientV24.builder()
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
										.digests(NotificationAttachmentDigests.builder().sha256(SHA256_BODY2).build())
										.contentType("application/json")
										.build())
								.build())
						.build()))
				.recipientType( NotificationRecipientV24.RecipientTypeEnum.PF )
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


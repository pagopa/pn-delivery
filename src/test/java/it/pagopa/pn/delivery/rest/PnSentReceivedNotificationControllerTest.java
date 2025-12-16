package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService.InternalAttachmentWithFileKey;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_FILEINFONOTFOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class PnSentReceivedNotificationControllerTest {

	private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
	private static final String CX_ID = "CX_ID";
	private static final String UID = "UID";
	private static final String PA_ID = "PA_ID";
	private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
	private static final String X_PAGOPA_PN_SRC_CH_DET = "sourceChannelDetails";
	private static final int DOCUMENT_INDEX = 0;
	private static final String REDIRECT_URL = "http://redirectUrl?token=fakeToken";
	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String FILENAME = "filename.pdf";
	private static final String REQUEST_ID = "VkdLVi1VS0hOLVZJQ0otMjAyMjA1LVAtMQ==";
	private static final String MANDATE_ID = "4fd712cd-8751-48ba-9f8c-471815146896";
	public static final String CX_TYPE_PF = "PF";
	public static final InternalAuthHeader INTERNAL_AUTH_HEADER = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, List.of("asdasd"), X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET);
	private static final String CX_TYPE_PA = "PA";
	private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
	private static final String IDEMPOTENCE_TOKEN = "idempotenceToken";
	private static final String PAGOPA = "PAGOPA";
	public static final String AAR_QR_CODE_VALUE_V1 = "WFFNVS1ETFFILVRWTVotMjAyMjA5LVYtMV9GUk1UVFI3Nk0wNkI3MTVFXzc5ZTA3NWMwLWIzY2MtNDc0MC04MjExLTllNTBjYTU4NjIzOQ";
	public static final String AAR_QR_CODE_VALUE_V2 = "VVFNWi1LTERHLUtEWVQtMjAyMjExLUwtMV9QRi00ZmM3NWRmMy0wOTEzLTQwN2UtYmRhYS1lNTAzMjk3MDhiN2RfZDA2ZjdhNDctNDJkMC00NDQxLWFkN2ItMTE4YmQ4NzlkOTJj";
	private static final String SENDER_ID = "CSRGGL44L13H501E";
	private static final String START_DATE = "2021-09-17T00:00:00.000Z";
	private static final String END_DATE = "2021-09-18T00:00:00.000Z";
	private static final NotificationStatusV26 STATUS = NotificationStatusV26.IN_VALIDATION;
	private static final String RECIPIENT_ID = "CGNNMO80A01H501M";
	public static final List<String> GROUPS = List.of("Group1", "Group2");
	public static final String DELIVERY_REQUESTS_PATH = "/delivery/v2.5/requests";
	public static final String DELIVERY_RECEIVED_PATH = "/delivery/v2.6/notifications/received/";
	public static final String DELIVERY_SENT_PATH = "/delivery/v2.7/notifications/sent/";

	@Autowired
    WebTestClient webTestClient;
	
	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private NotificationAttachmentService attachmentService;

	@MockBean
	private NotificationQRService qrService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@SpyBean
	private ModelMapper modelMapper;

	@Test
	void getSentNotificationSuccess() {
		// Given		
		InternalNotification notification = newNotification();
		
		// When
		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );
				
		// Then		
		webTestClient.get()
			.uri( DELIVERY_SENT_PATH + IUN  )
			.accept( MediaType.ALL )
			.header(HttpHeaders.ACCEPT, "application/json")
			.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
			.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
			.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
			.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
			.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(FullSentNotificationV27.class);
		
		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck(IUN, PA_ID, GROUPS);
	}

	@Test
	void getSentNotificationNotFoundCauseIN_VALIDATION() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatus( NotificationStatusV26.IN_VALIDATION );

		// When
		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( DELIVERY_SENT_PATH + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isNotFound();

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck(IUN, PA_ID, GROUPS);
	}


	@Test
	void getSentNotificationNotFoundCauseREFUSED() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatus( NotificationStatusV26.REFUSED );

		// When
		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( DELIVERY_SENT_PATH + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isNotFound();

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck(IUN, PA_ID, GROUPS);
	}


	@Test
	void getNotificationRequestStatusByRequestIdIN_VALIDATION() {
		// Given
		InternalNotification notification = newNotification();
		notification.setNotificationStatusHistory( null );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponseV25.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID, GROUPS );
	}

	@Test
	void testTimeLine(){
		TimelineElementDetailsV27 actualTimelineElementDetails = new TimelineElementDetailsV27();
		actualTimelineElementDetails.aarKey("Aar Key");
		actualTimelineElementDetails.amount(10);
		actualTimelineElementDetails.analogCost(1);
		ArrayList<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.AttachmentDetails> attachments = new ArrayList<>();
		actualTimelineElementDetails.attachments(attachments);
		actualTimelineElementDetails
				.attemptDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.completionWorkflowDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.contactPhase(ContactPhase.CHOOSE_DELIVERY);
		actualTimelineElementDetails.creditorTaxId("42");
		DelegateInfo delegateInfo = new DelegateInfo("42", "42", "01234567-89AB-CDEF-FEDC-BA9876543210", "2020-03-01",
				"Denomination", RecipientType.PF);

		actualTimelineElementDetails.delegateInfo(delegateInfo);
		actualTimelineElementDetails.deliveryDetailCode("Delivery Detail Code");
		actualTimelineElementDetails.deliveryFailureCause("Delivery Failure Cause");
		actualTimelineElementDetails.deliveryMode(DeliveryMode.DIGITAL);
		DigitalAddress digitalAddress = new DigitalAddress(
				"PEC", "fake@mail.it");

		actualTimelineElementDetails.digitalAddress(digitalAddress);
		actualTimelineElementDetails.digitalAddressSource(DigitalAddressSource.PLATFORM);
		actualTimelineElementDetails.endWorkflowStatus(EndWorkflowStatus.SUCCESS);
		actualTimelineElementDetails.envelopeWeight(3);
		actualTimelineElementDetails
				.eventTimestamp(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.generatedAarUrl("https://example.org/example");
		actualTimelineElementDetails.idF24("Id F24");
		actualTimelineElementDetails.ioSendMessageResult(IoSendMessageResult.NOT_SENT_OPTIN_ALREADY_SENT);
		actualTimelineElementDetails
				.lastAttemptDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.legalFactGenerationDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.legalFactId("42");
		actualTimelineElementDetails.legalfactId("42");
		PhysicalAddress newAddress = new PhysicalAddress("At", "42 Main St", "42 Main St", "21654",
				"Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.newAddress(newAddress);
		actualTimelineElementDetails.nextDigitalAddressSource(DigitalAddressSource.PLATFORM);
		actualTimelineElementDetails
				.nextLastAttemptMadeForSource(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.nextSourceAttemptsMade(1);
		PhysicalAddress normalizedAddress = new PhysicalAddress("At", "42 Main St", "42 Main St",
				"21654", "Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.normalizedAddress(normalizedAddress);
		actualTimelineElementDetails.noticeCode("Notice Code");
		actualTimelineElementDetails.notificationCost(1L);
		actualTimelineElementDetails
				.notificationDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.numberOfPages(10);
		PhysicalAddress oldAddress = new PhysicalAddress("At", "42 Main St", "42 Main St", "21654",
				"Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.oldAddress(oldAddress);
		actualTimelineElementDetails.paymentSourceChannel("Payment Source Channel");
		PhysicalAddress physicalAddress = new PhysicalAddress("At", "42 Main St", "42 Main St",
				"21654", "Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.physicalAddress(physicalAddress);
		actualTimelineElementDetails.prepareRequestId("42");
		actualTimelineElementDetails.productType("Product Type");
		actualTimelineElementDetails.raddTransactionId("42");
		actualTimelineElementDetails.raddType("Radd Type");
		actualTimelineElementDetails.reason("Just cause");
		actualTimelineElementDetails.reasonCode("Just cause");
		actualTimelineElementDetails.recIndex(1);
		actualTimelineElementDetails.recipientType(RecipientType.PF);
		ArrayList<NotificationRefusedErrorV27> refusalReasons = new ArrayList<>();
		actualTimelineElementDetails.refusalReasons(refusalReasons);
		actualTimelineElementDetails.registeredLetterCode("Registered Letter Code");
		actualTimelineElementDetails.relatedRequestId("42");
		actualTimelineElementDetails.responseStatus(ResponseStatus.OK);
		actualTimelineElementDetails.retryNumber(10);
		actualTimelineElementDetails
				.schedulingAnalogDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.schedulingDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.sendDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.sendRequestId("42");
		ArrayList<SendingReceipt> sendingReceipts = new ArrayList<>();
		actualTimelineElementDetails.sendingReceipts(sendingReceipts);
		actualTimelineElementDetails.sentAttemptMade(1);
		actualTimelineElementDetails.serviceLevel(ServiceLevel.AR_REGISTERED_LETTER);
		actualTimelineElementDetails.setAarKey("Aar Key");
		actualTimelineElementDetails.setAmount(10);
		actualTimelineElementDetails.setAnalogCost(1);
		ArrayList<AttachmentDetails> attachments2 = new ArrayList<>();
		actualTimelineElementDetails.setAttachments(attachments2);
		OffsetDateTime attemptDate = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
		actualTimelineElementDetails.setAttemptDate(attemptDate);
		OffsetDateTime completionWorkflowDate = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT,
				ZoneOffset.UTC);
		actualTimelineElementDetails.setCompletionWorkflowDate(completionWorkflowDate);
		actualTimelineElementDetails.setContactPhase(ContactPhase.CHOOSE_DELIVERY);
		actualTimelineElementDetails.setCreditorTaxId("42");
		DelegateInfo delegateInfo2 = new DelegateInfo("42", "42", "01234567-89AB-CDEF-FEDC-BA9876543210", "2020-03-01",
				"Denomination", RecipientType.PF);

		actualTimelineElementDetails.setDelegateInfo(delegateInfo2);
		actualTimelineElementDetails.setDeliveryDetailCode("Delivery Detail Code");
		actualTimelineElementDetails.setDeliveryFailureCause("Delivery Failure Cause");
		actualTimelineElementDetails.setDeliveryMode(DeliveryMode.DIGITAL);
		DigitalAddress digitalAddress2 = new DigitalAddress(
				"PEC", "fake@pec.it");

		actualTimelineElementDetails.setDigitalAddress(digitalAddress2);
		actualTimelineElementDetails.setDigitalAddressSource(DigitalAddressSource.PLATFORM);
		actualTimelineElementDetails.setEndWorkflowStatus(EndWorkflowStatus.SUCCESS);
		actualTimelineElementDetails.setEnvelopeWeight(3);
		OffsetDateTime eventTimestamp = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
		actualTimelineElementDetails.setEventTimestamp(eventTimestamp);
		actualTimelineElementDetails.setGeneratedAarUrl("https://example.org/example");
		actualTimelineElementDetails.setIdF24("Id F24");
		actualTimelineElementDetails.setIoSendMessageResult(IoSendMessageResult.NOT_SENT_OPTIN_ALREADY_SENT);
		actualTimelineElementDetails.setIsAvailable(true);
		OffsetDateTime lastAttemptDate = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
		actualTimelineElementDetails.setLastAttemptDate(lastAttemptDate);
		OffsetDateTime legalFactGenerationDate = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT,
				ZoneOffset.UTC);
		actualTimelineElementDetails.setLegalFactGenerationDate(legalFactGenerationDate);
		actualTimelineElementDetails.setLegalFactId("42");
		actualTimelineElementDetails.setLegalfactId("42");
		PhysicalAddress newAddress2 = new PhysicalAddress("At", "42 Main St", "42 Main St", "21654",
				"Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.setNewAddress(newAddress2);
		actualTimelineElementDetails.setNextDigitalAddressSource(DigitalAddressSource.PLATFORM);
		OffsetDateTime nextLastAttemptMadeForSource = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT,
				ZoneOffset.UTC);
		actualTimelineElementDetails.setNextLastAttemptMadeForSource(nextLastAttemptMadeForSource);
		actualTimelineElementDetails.setNextSourceAttemptsMade(1);
		PhysicalAddress normalizedAddress2 = new PhysicalAddress("At", "42 Main St", "42 Main St",
				"21654", "Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.setNormalizedAddress(normalizedAddress2);
		actualTimelineElementDetails.setNoticeCode("Notice Code");
		actualTimelineElementDetails.setNotificationCost(1L);
		OffsetDateTime notificationDate = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
		actualTimelineElementDetails.setNotificationDate(notificationDate);
		actualTimelineElementDetails.setPhysicalAddress(new PhysicalAddress("At", "42 Main St",
				"42 Main St", "21654", "Municipality", "Municipality Details", "Province", "Foreign State"));
		actualTimelineElementDetails.setNumberOfPages(10);
		PhysicalAddress oldAddress2 = new PhysicalAddress("At", "42 Main St", "42 Main St", "21654",
				"Municipality", "Municipality Details", "Province", "Foreign State");

		actualTimelineElementDetails.setOldAddress(oldAddress2);
		actualTimelineElementDetails.setPaymentSourceChannel("Payment Source Channel");
		actualTimelineElementDetails.setPrepareRequestId("42");
		actualTimelineElementDetails.setProductType("Product Type");
		actualTimelineElementDetails.setRaddTransactionId("42");
		actualTimelineElementDetails.setRaddType("Radd Type");
		actualTimelineElementDetails.setReason("Just cause");
		actualTimelineElementDetails.setReasonCode("Just cause");
		actualTimelineElementDetails.setRecIndex(1);
		actualTimelineElementDetails.setRecipientType(RecipientType.PF);
		ArrayList<NotificationRefusedErrorV27> refusalReasons2 = new ArrayList<>();
		actualTimelineElementDetails.setRefusalReasons(refusalReasons2);
		actualTimelineElementDetails.setRegisteredLetterCode("Registered Letter Code");
		actualTimelineElementDetails.setRelatedRequestId("42");
		actualTimelineElementDetails.setResponseStatus(ResponseStatus.OK);
		actualTimelineElementDetails.setRetryNumber(10);
		actualTimelineElementDetails
				.setSchedulingAnalogDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.setSchedulingDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails
				.setSendDate(OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
		actualTimelineElementDetails.setSendRequestId("42");
		ArrayList<SendingReceipt> sendingReceipts2 = new ArrayList<>();
		actualTimelineElementDetails.setSendingReceipts(sendingReceipts2);
		actualTimelineElementDetails.setSentAttemptMade(1);
		actualTimelineElementDetails.setServiceLevel(ServiceLevel.AR_REGISTERED_LETTER);
		actualTimelineElementDetails.setShouldRetry(true);
		actualTimelineElementDetails.setUncertainPaymentDate(true);
		actualTimelineElementDetails.shouldRetry(true);
		actualTimelineElementDetails.uncertainPaymentDate(true);
		testingTimeLine(actualTimelineElementDetails);
		testingTimeLine1(actualTimelineElementDetails);
		testingTimeLine2(actualTimelineElementDetails);
		testingTimeLine3(actualTimelineElementDetails);
	}

	void testingTimeLine3(TimelineElementDetailsV27 actualTimelineElementDetails){
		assertNotNull(actualTimelineElementDetails.getLegalFactGenerationDate());
		assertEquals("42", actualTimelineElementDetails.getLegalFactId());
		assertEquals("42", actualTimelineElementDetails.getLegalfactId());
		PhysicalAddress newAddress3 = actualTimelineElementDetails.getNewAddress();
		PhysicalAddress normalizedAddress3 = actualTimelineElementDetails.getNormalizedAddress();
		assertEquals(normalizedAddress3, newAddress3);
		PhysicalAddress oldAddress3 = actualTimelineElementDetails.getOldAddress();
		assertEquals(oldAddress3, newAddress3);
		PhysicalAddress physicalAddress2 = actualTimelineElementDetails.getPhysicalAddress();
		assertEquals(physicalAddress2, newAddress3);
		assertEquals(DigitalAddressSource.PLATFORM, actualTimelineElementDetails.getNextDigitalAddressSource());
		assertNotNull(actualTimelineElementDetails.getNextLastAttemptMadeForSource());
		assertEquals(1, actualTimelineElementDetails.getNextSourceAttemptsMade().intValue());
		assertEquals(oldAddress3, normalizedAddress3);
		assertEquals(physicalAddress2, normalizedAddress3);
		assertEquals(1L, actualTimelineElementDetails.getNotificationCost().longValue());
		assertNotNull(actualTimelineElementDetails.getNotificationDate());
		assertSame(physicalAddress2, actualTimelineElementDetails.getPhysicalAddress());
		assertEquals(10, actualTimelineElementDetails.getNumberOfPages().intValue());
		assertEquals(physicalAddress2, oldAddress3);
		assertEquals("Payment Source Channel", actualTimelineElementDetails.getPaymentSourceChannel());
		assertEquals("42", actualTimelineElementDetails.getPrepareRequestId());
		assertEquals("Product Type", actualTimelineElementDetails.getProductType());
		assertEquals("42", actualTimelineElementDetails.getRaddTransactionId());
		assertEquals("Radd Type", actualTimelineElementDetails.getRaddType());
		assertEquals("Just cause", actualTimelineElementDetails.getReason());
		assertEquals("Just cause", actualTimelineElementDetails.getReasonCode());
		assertEquals(1, actualTimelineElementDetails.getRecIndex().intValue());
		assertEquals(RecipientType.PF, actualTimelineElementDetails.getRecipientType());
	}

	void testingTimeLine2(TimelineElementDetailsV27 actualTimelineElementDetails){
		assertEquals("Aar Key", actualTimelineElementDetails.getAarKey());
		assertEquals(10, actualTimelineElementDetails.getAmount().intValue());
		assertEquals(1, actualTimelineElementDetails.getAnalogCost().intValue());
		assertNotNull(actualTimelineElementDetails.getAttemptDate());
		assertNotNull(actualTimelineElementDetails.getCompletionWorkflowDate());
		assertEquals(ContactPhase.CHOOSE_DELIVERY, actualTimelineElementDetails.getContactPhase());
		assertEquals("42", actualTimelineElementDetails.getCreditorTaxId());
		assertEquals("Delivery Detail Code", actualTimelineElementDetails.getDeliveryDetailCode());
		assertEquals("Delivery Failure Cause", actualTimelineElementDetails.getDeliveryFailureCause());
		assertEquals(DeliveryMode.DIGITAL, actualTimelineElementDetails.getDeliveryMode());
		DigitalAddress digitalAddress3 = actualTimelineElementDetails.getDigitalAddress();
		assertEquals(DigitalAddressSource.PLATFORM, actualTimelineElementDetails.getDigitalAddressSource());
		assertEquals(EndWorkflowStatus.SUCCESS, actualTimelineElementDetails.getEndWorkflowStatus());
		assertEquals(3, actualTimelineElementDetails.getEnvelopeWeight().intValue());
		assertNotNull(actualTimelineElementDetails.getEventTimestamp());
		assertEquals("https://example.org/example", actualTimelineElementDetails.getGeneratedAarUrl());
		assertEquals("Id F24", actualTimelineElementDetails.getIdF24());
		assertEquals(IoSendMessageResult.NOT_SENT_OPTIN_ALREADY_SENT,
				actualTimelineElementDetails.getIoSendMessageResult());
		assertTrue(actualTimelineElementDetails.getIsAvailable());
		assertNotNull(actualTimelineElementDetails.getLastAttemptDate());
		assertEquals("Registered Letter Code", actualTimelineElementDetails.getRegisteredLetterCode());
		assertEquals("42", actualTimelineElementDetails.getRelatedRequestId());
		TimelineElementDetailsV27 actualIsAvailableResult = actualTimelineElementDetails.isAvailable(true);
		assertSame(actualTimelineElementDetails, actualIsAvailableResult);
		assertEquals("Notice Code", actualTimelineElementDetails.getNoticeCode());
	}

	void testingTimeLine1(TimelineElementDetailsV27 timelineElementDetails){
		Assertions.assertNotNull(timelineElementDetails.getLegalFactId());
		Assertions.assertNotNull(timelineElementDetails.getNormalizedAddress());
		Assertions.assertNotNull(timelineElementDetails.getGeneratedAarUrl());
		Assertions.assertNotNull(timelineElementDetails.getPhysicalAddress());
		Assertions.assertNotNull(timelineElementDetails.getLegalfactId());
		Assertions.assertNotNull(timelineElementDetails.getEndWorkflowStatus());
		Assertions.assertNotNull(timelineElementDetails.getCompletionWorkflowDate());
		Assertions.assertNotNull(timelineElementDetails.getLegalFactGenerationDate());
		Assertions.assertNotNull(timelineElementDetails.getDigitalAddress());
		Assertions.assertNotNull(timelineElementDetails.getDigitalAddressSource());
		Assertions.assertNotNull(timelineElementDetails.getIsAvailable());
		Assertions.assertNotNull(timelineElementDetails.getAttemptDate());
		Assertions.assertNotNull(timelineElementDetails.getEventTimestamp());
		Assertions.assertNotNull(timelineElementDetails.getRaddType());
		Assertions.assertNotNull(timelineElementDetails.getRaddTransactionId());
	}

	void testingTimeLine(TimelineElementDetailsV27 timelineElementDetails){
		Assertions.assertNotNull(timelineElementDetails.getResponseStatus());
		Assertions.assertNotNull(timelineElementDetails.getNextLastAttemptMadeForSource());
		Assertions.assertNotNull(timelineElementDetails.getNextSourceAttemptsMade());
		Assertions.assertNotNull(timelineElementDetails.getNextDigitalAddressSource());
		Assertions.assertNotNull(timelineElementDetails.getRetryNumber());
		Assertions.assertNotNull(timelineElementDetails.getIoSendMessageResult());
		Assertions.assertNotNull(timelineElementDetails.getSchedulingDate());
		Assertions.assertNotNull(timelineElementDetails.getLastAttemptDate());
		Assertions.assertNotNull(timelineElementDetails.getRefusalReasons());
		Assertions.assertNotNull(timelineElementDetails.getSendDate());
		Assertions.assertNotNull(timelineElementDetails.getSentAttemptMade());
		Assertions.assertNotNull(timelineElementDetails.getContactPhase());
		Assertions.assertNotNull(timelineElementDetails.getDeliveryMode());
		Assertions.assertNotNull(timelineElementDetails.getNotificationCost());
		Assertions.assertNotNull(timelineElementDetails.getDelegateInfo());
		Assertions.assertNotNull(timelineElementDetails.getOldAddress());
	}

	@Test
	void getNotificationRequestStatusByRequestIdREFUSED() {
		// Given

		InternalNotification notification = newNotification();
		notification.setNotificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElementV26.builder()
						.status( NotificationStatusV26.REFUSED )
				.build() ) );
		notification.setTimeline( Collections.singletonList( TimelineElementV28.builder()
						.category( TimelineElementCategoryV27.REQUEST_REFUSED )
						.details( TimelineElementDetailsV27.builder()
								.refusalReasons( Collections.singletonList( NotificationRefusedErrorV27.builder()
												.errorCode( "FILE_NOTFOUND" )
												.detail( "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869" )
										.build() ) )
								.build() )
				.build() ) );

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponseV25.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID, GROUPS );
	}

	@Test
	void getNotificationRequestStatusByRequestIdSuccess() {
		// Given
		InternalNotification notification = newNotification();

		Mockito.when( svc.getNotificationInformationWithSenderIdCheck( anyString(), anyString(), anyList() ) ).thenReturn( notification );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.queryParam("notificationRequestId", REQUEST_ID)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponseV25.class );

		Mockito.verify( svc ).getNotificationInformationWithSenderIdCheck( new String(Base64Utils.decodeFromString(REQUEST_ID), StandardCharsets.UTF_8), PA_ID, GROUPS );
	}

	@Test
	void getNotificationRequestStatusByProtocolOnlyFailure() {
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.queryParam("paProtocolNumber", PA_PROTOCOL_NUMBER)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void getNotificationRequestStatusWithoutProtocol() {
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void getNotificationRequestStatusByProtocolAndIdempotenceSuccess() {
		// Given
		InternalNotification notification = newNotification();

		Mockito.when( svc.getNotificationInformation( anyString(), anyString(), anyString(), anyList() ) ).thenReturn( notification );

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path(DELIVERY_REQUESTS_PATH)
								.queryParam("paProtocolNumber", PA_PROTOCOL_NUMBER)
								.queryParam( "idempotenceToken", IDEMPOTENCE_TOKEN )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get( 0 ) )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER,  GROUPS.get( 1 ) )
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody( NewNotificationRequestStatusResponseV25.class );

		Mockito.verify( svc ).getNotificationInformation( PA_ID, PA_PROTOCOL_NUMBER, IDEMPOTENCE_TOKEN, GROUPS );
	}

	@Test
	void getReceivedNotificationSuccess() {
		// Given
		InternalNotification notification = newNotification();

		// When
		Mockito.when(svc.getNotificationAndNotifyViewedEvent(Mockito.anyString(), Mockito.any(InternalAuthHeader.class), eq(null)))
				.thenReturn( notification );

		// Then
		webTestClient.get()
				.uri( DELIVERY_RECEIVED_PATH + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotificationV26.class);

		Mockito.verify(svc).getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, null);
	}

	@Test
	void getReceivedNotificationFailure() {

		// When
		Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.any( InternalAuthHeader.class ), eq( null )) )
				.thenThrow(new PnNotificationNotFoundException("test"));

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN  )
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationByDelegateSuccess() {
		// Given
		InternalNotification notification = newNotification();
		// When
		Mockito.when(svc.getNotificationAndNotifyViewedEvent(anyString(), any(InternalAuthHeader.class), anyString()))
				.thenReturn(notification);

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( DELIVERY_RECEIVED_PATH + IUN )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(FullReceivedNotificationV26.class);

		Mockito.verify(svc).getNotificationAndNotifyViewedEvent(IUN, INTERNAL_AUTH_HEADER, MANDATE_ID);
	}

	@Test
	void getSentNotificationDocumentsWithPresignedSuccess() {
		InternalAttachmentWithFileKey response = InternalAttachmentWithFileKey.of(NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build(), "MockFileKey");

		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PA, PA_ID, UID, List.of("asdasd"));

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirectWithFileKey(
						anyString(),
						any(InternalAuthHeader.class),
						isNull(),
						Mockito.anyInt(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirectWithFileKey( IUN, internalAuthHeader, null, DOCUMENT_INDEX, false );
	}

	@Test
	void getReceivedNotificationDocumentsWithPresignedSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
						anyString(),
						any(InternalAuthHeader.class),
						Mockito.isNull(),
						Mockito.anyInt(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
		        .isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_INDEX, true );
	}

	@Test
	void getReceivedNotificationDocumentsWithRetryAfterSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( null )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.retryAfter( 3600 )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
				anyString(),
				any(InternalAuthHeader.class),
				Mockito.isNull(),
				Mockito.anyInt(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX)
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				//.header( "location" , REDIRECT_URL )
				.exchange()
				.expectStatus()
				//.is3xxRedirection()
				.isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, DOCUMENT_INDEX, true );
	}

	@Test
	void getReceivedNotificationDocumentsWithMandateIdSuccess() {

		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( true );
		Mockito.when( attachmentService.downloadDocumentWithRedirect(
				anyString(),
				any(InternalAuthHeader.class),
				anyString(),
				Mockito.anyInt(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/" + IUN + "/attachments/documents/" + DOCUMENT_INDEX )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadDocumentWithRedirect( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID, DOCUMENT_INDEX, true );
	}

	@Test
	void getSentNotificationAttachmentSuccess() {
		//Given
		InternalAttachmentWithFileKey response = InternalAttachmentWithFileKey.of(NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build(), "MockFileFey");

		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PA, CX_ID, UID, List.of("asdasd"));

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirectWithFileKey(
						anyString(),
						any(InternalAuthHeader.class),
						isNull(),
						Mockito.anyInt(),
						anyString(),
						any(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",PAGOPA))
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header(PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirectWithFileKey( IUN, internalAuthHeader, null,  0, PAGOPA, null,false);
	}

	@Test
	void getSentNotificationAttachmentSuccessFileKeyNull() {
		//Given
		InternalAttachmentWithFileKey response = InternalAttachmentWithFileKey.of(NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build(), null);

		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PA, CX_ID, UID, List.of("asdasd"));

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirectWithFileKey(
				anyString(),
				any(InternalAuthHeader.class),
				isNull(),
				Mockito.anyInt(),
				anyString(),
				any(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",PAGOPA))
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PA)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirectWithFileKey( IUN, internalAuthHeader, null,  0, PAGOPA, null,false);
	}

	@Test
	void getSentNotificationAttachmentFailure() {
		// When
		Mockito.doThrow( new PnNotificationNotFoundException("Simulated Error") )
				.when( attachmentService )
				.downloadAttachmentWithRedirectWithFileKey( IUN, new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd")), null, 0, PAGOPA, null,false );

		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/payment/{recipientIdx}/{attachmentName}".replace("{iun}",IUN).replace("{recipientIdx}","0").replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getSentNotificationDocumentFailure() {
		// When
		Mockito.when( attachmentService.downloadDocumentWithRedirectWithFileKey( IUN, new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd")), null, 0, false ))
				.thenThrow( new PnNotificationNotFoundException("Simulated Error") );

		webTestClient.get()
				.uri( "/delivery/notifications/sent/{iun}/attachments/documents/{docIdx}".replace("{iun}",IUN).replace("{docIdx}","0"))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationDocumentFailure() {
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, PA_ID, UID, List.of("asdasd"), X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET);
		// When
		Mockito.when( attachmentService.downloadDocumentWithRedirect( IUN, internalAuthHeader, null, 0, true ))
				.thenThrow( new PnNotificationNotFoundException("Simulated Error") );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/documents/{docIdx}".replace("{iun}",IUN).replace("{docIdx}","0"))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationAttachmentFailure() {
		// When
		InternalAuthHeader internalAuthHeader = new InternalAuthHeader(CX_TYPE_PF, CX_ID, UID, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET);
		Mockito.doThrow( new PnNotificationNotFoundException("Simulated Error") )
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, internalAuthHeader, null,null, PAGOPA, null, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	@Test
	void getReceivedNotificationAttachmentBadRequestFailure() {
		// When
		Mockito.doThrow( new PnBadRequestException("Request took too long to complete.", "test", ERROR_CODE_DELIVERY_FILEINFONOTFOUND))
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null,null, PAGOPA, null, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}


	@Test
	void getReceivedNotificationAttachmentInternalErrorFailure() {
		// When
		Mockito.doThrow( new PnInternalException("Simulated Error", "test") )
				.when( attachmentService )
				.downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null,null, PAGOPA, null, true );

		webTestClient.get()
				.uri( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",PAGOPA))
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.is5xxServerError();
	}

	@Test
	void getReceivedNotificationAttachmentSuccess() {
		//Given
		String pagopa = "PAGOPA";
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirect(
						anyString(),
						any( InternalAuthHeader.class ),
						anyString(),
						Mockito.isNull(),
						anyString(),
						Mockito.any(),
						Mockito.anyBoolean()
				)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",pagopa) )
								.queryParam("mandateId", MANDATE_ID)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, MANDATE_ID, null, pagopa, null, true);
	}

	@Test
	void getReceivedNotificationAttachmentSuccessNoMandate() {
		//Given
		String pagopa = "PAGOPA";
		NotificationAttachmentDownloadMetadataResponse response = NotificationAttachmentDownloadMetadataResponse.builder()
				.url( REDIRECT_URL )
				.contentType( "application/pdf" )
				.sha256( SHA256_BODY )
				.filename( FILENAME )
				.build();

		// When
		//Mockito.when(cfg.isDownloadWithPresignedUrl()).thenReturn( false );
		Mockito.when( attachmentService.downloadAttachmentWithRedirect(
				Mockito.anyString(),
				Mockito.any( InternalAuthHeader.class ),
				Mockito.isNull(),
				Mockito.isNull(),
				Mockito.anyString(),
				Mockito.any(),
				Mockito.anyBoolean()
		)).thenReturn( response );

		// Then
		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received/{iun}/attachments/payment/{attachmentName}".replace("{iun}",IUN).replace("{attachmentName}",pagopa) )
								//.queryParam("mandateId", null)
								.build())
				.accept( MediaType.ALL )
				.header(HttpHeaders.ACCEPT, "application/json")
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DET)
				.exchange()
				.expectStatus()
				.isOk();

		Mockito.verify( attachmentService ).downloadAttachmentWithRedirect( IUN, INTERNAL_AUTH_HEADER, null, null, pagopa, null, true);
	}

	@Test
	void searchSentNotificationFailure() {
		// When
		Mockito.doThrow(new PnInternalException("Simulated Error"))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/sent" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.is5xxServerError();

	}

	@Test
	void searchSentNotificationValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/sent" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();

	}

	@Test
	void searchReceivedNotificationValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotification(any(InputSearchNotificationDto.class), any(), any());

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/delivery/notifications/received" )
								.queryParam("startDate", "2022-08-25T12:30:28Z" )
								.queryParam( "endDate",  "2022-08-26T12:30:28Z" )
								.build())
				.header( PnDeliveryRestConstants.CX_ID_HEADER, PA_ID )
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus()
				.isBadRequest();

	}

	@Test
	void getNotificationQRV1Success(){

		//Given
		ResponseCheckAarMandateDto QrMandateResponse = ResponseCheckAarMandateDto.builder()
				.iun( "iun" )
				.build();

		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V1)
				.build();

		//When
		Mockito.when( qrService.getNotificationByQRWithMandate(Mockito.any( RequestCheckAarMandateDto.class ), anyString(), anyString(), any()))
				.thenReturn( QrMandateResponse );

		webTestClient.post()
				.uri( "/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(ResponseCheckAarMandateDto.class );

		//Then
		Mockito.verify( qrService ).getNotificationByQRWithMandate( dto, CX_TYPE_PF, CX_ID, null);
	}

	@Test
	void getNotificationQRV2Success(){

		//Given
		ResponseCheckAarMandateDto QrMandateResponse = ResponseCheckAarMandateDto.builder()
				.iun( "iun" )
				.build();

		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V2)
				.build();

		//When
		Mockito.when( qrService.getNotificationByQRWithMandate(Mockito.any( RequestCheckAarMandateDto.class ), anyString(), anyString(), any()))
				.thenReturn( QrMandateResponse );

		webTestClient.post()
				.uri( "/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(ResponseCheckAarMandateDto.class );

		//Then
		Mockito.verify( qrService ).getNotificationByQRWithMandate( dto, CX_TYPE_PF, CX_ID, null);
	}

	@Test
	void getNotificationQRFailure() {
		RequestCheckAarMandateDto dto = RequestCheckAarMandateDto.builder()
				.aarQrCodeValue(AAR_QR_CODE_VALUE_V1)
				.build();

		//When
		Mockito.when(qrService.getNotificationByQRWithMandate(Mockito.any(RequestCheckAarMandateDto.class), anyString(), anyString(), any()))
				.thenThrow(new PnNotFoundException("test", "test", "test"));

		webTestClient.post()
				.uri("/delivery/notifications/received/check-aar-qr-code")
				.contentType(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, CX_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, CX_TYPE_PF)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(dto), RequestCheckAarMandateDto.class)
				.exchange()
				.expectStatus()
				.isNotFound();
	}

	private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
		headers.add( "Pragma", "no-cache" );
		headers.add( "Expires", "0" );
		return headers;
	}

	private InternalNotification newNotification() {
		InternalNotification internalNotification = new InternalNotification();
		internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
		internalNotification.setSentAt(OffsetDateTime.now());
		internalNotification.setRecipients(
				List.of(
						NotificationRecipient.builder()
								.internalId("internalId")
								.recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
								.taxId("taxId")
								.physicalAddress(it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress.builder().build())
								.digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder().build())
								.payments(List.of(NotificationPaymentInfo.builder().build()))
								.build()));
		internalNotification.setIun("IUN_01");
		internalNotification.setPaProtocolNumber("protocol_01");
		internalNotification.setSubject("Subject 01");
		internalNotification.setCancelledIun("IUN_05");
		internalNotification.setCancelledIun("IUN_00");
		internalNotification.setSenderPaId("PA_ID");
		internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
		internalNotification.setRecipients(Collections.singletonList(
				NotificationRecipient.builder()
						.taxId("Codice Fiscale 01")
						.denomination("Nome Cognome/Ragione Sociale")
						.internalId( "recipientInternalId" )
						.digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address("account@dominio.it")
								.build()).build()));
		return internalNotification;
	}
	@Test
	void searchNotificationDelegatedFailure() {
		// When
		Mockito.doThrow(new PnInternalException("Simulated Error"))
				.when(svc)
				.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class));

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/" + PnDeliveryRestConstants.NOTIFICATION_RECEIVED_DELEGATED_PATH )
								.queryParam("startDate", START_DATE)
								.queryParam("endDate", END_DATE)
								.queryParam("recipientId", RECIPIENT_ID)
								.queryParam("status", STATUS)
								.build())
				.accept(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0)+","+GROUPS.get(1))
				.exchange()
				.expectStatus()
				.is5xxServerError();

	}
	@Test
	void searchNotificationDelegatedValidationFailure() {
		// When
		Mockito.doThrow(new PnValidationException("Simulated Error", Collections.emptySet()))
				.when(svc)
				.searchNotificationDelegated(any(InputSearchNotificationDelegatedDto.class));

		webTestClient.get()
				.uri(uriBuilder ->
						uriBuilder
								.path( "/" + PnDeliveryRestConstants.NOTIFICATION_RECEIVED_DELEGATED_PATH )
								.queryParam("startDate", START_DATE)
								.queryParam("endDate", END_DATE)
								.queryParam("recipientId", RECIPIENT_ID)
								.queryParam("status", STATUS)
								.build())
				.accept(MediaType.APPLICATION_JSON)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, SENDER_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0)+","+GROUPS.get(1))
				.exchange()
				.expectStatus()
				.isBadRequest();

	}

	@Test
	void getReceivedNotificationDocumentWithNotificationCancelledTest() {
		webTestClient.get()
				.uri("/delivery/v2.3/notifications/received/{iun}/attachments/documents/{docIdx}", IUN, 1)
				.header( PnDeliveryRestConstants.CX_ID_HEADER, RECIPIENT_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, UID)
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
				.exchange()
				.expectStatus().isNotFound();
	}

}

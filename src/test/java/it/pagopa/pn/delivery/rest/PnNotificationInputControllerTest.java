package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.TaxonomyCodeDaoDynamo;
import it.pagopa.pn.delivery.models.TaxonomyCodeDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.utils.PreloadRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_TAXONOMYCODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@WebFluxTest(PnNotificationInputController.class)
class PnNotificationInputControllerTest {

	private static final String PA_ID = "paId";
	private static final String PA_ID_DEFAULT = "default";
	private static final String IUN = "IUN";
	public static final Integer MAX_NUMBER_REQUESTS = 1;
	private static final String SECRET = "secret";
	private static final String URL = "url";
	public static final List<String> GROUPS = List.of("Group1", "Group2");
	private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
	private static final String X_PAGOPA_PN_SRC_CH_DETAILS = "sourceChannelDetails";
	private static final String FILE_SHA_256 = "jezIVxlG1M1woCSUngM6KipUN3/p8cG5RMIPnuEanlE=";
	public static final String DELIVERY_REQUESTS_PATH = "/delivery/v2.5/requests";

	@Autowired
    WebTestClient webTestClient;

	@SpyBean
	private ModelMapper modelMapper;
	
	@MockBean
	private NotificationReceiverService deliveryService;

	@MockBean
	private NotificationAttachmentService attachmentService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@MockBean
	TaxonomyCodeDaoDynamo taxonomyCodeDaoDynamo;

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postSuccess(boolean isCheckEnabled) throws PnIdConflictException {
		// Given
		NewNotificationRequestV25 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
						.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
						.paProtocolNumber("protocol_number").build();

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();

		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when(deliveryService.receiveNotification(
						Mockito.anyString(),
						any( NewNotificationRequestV25.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull())
				).thenReturn( savedNotification );

		// Then
		webTestClient.post()
                .uri(DELIVERY_REQUESTS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(notificationRequest), NewNotificationRequestV25.class)
                .header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
                .exchange()
                .expectStatus().isAccepted();
		
		Mockito.verify( deliveryService ).receiveNotification(
						PA_ID,
						notificationRequest,
						X_PAGOPA_PN_SRC_CH,
						null,
						GROUPS,
						null);

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}
	}

/*	@Test
	void postSuccessWithTaxonomyCodeCheck() throws PnIdConflictException {
		// Given
		NewNotificationRequestV23 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber("protocol_number").build();

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();

		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), "default")).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when(deliveryService.receiveNotification(
				Mockito.anyString(),
				Mockito.any( NewNotificationRequestV23.class ),
				Mockito.anyString(),
				Mockito.isNull(),
				Mockito.anyList(),
				Mockito.isNull())
		).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri("/delivery/v2.3/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV23.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
				PA_ID,
				notificationRequest,
				X_PAGOPA_PN_SRC_CH,
				null,
				GROUPS,
				null);

		Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), "default");
	}*/

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postSuccessWithSourceChannelDetails(boolean isCheckEnabled) throws PnIdConflictException {
		// Given
		NewNotificationRequestV25 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber("protocol_number").build();

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();

		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when(deliveryService.receiveNotification(
				Mockito.anyString(),
				any( NewNotificationRequestV25.class ),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyList(),
				Mockito.isNull())
		).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DETAILS )
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
				PA_ID,
				notificationRequest,
				X_PAGOPA_PN_SRC_CH,
				X_PAGOPA_PN_SRC_CH_DETAILS,
				GROUPS,
				null);

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postSuccessWithNotificationVersion(boolean isCheckEnabled) throws PnIdConflictException {
		// Given
		NewNotificationRequestV25 notificationRequest = newNotificationRequest();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber("protocol_number").build();

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();

		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when(deliveryService.receiveNotification(
				Mockito.anyString(),
				any( NewNotificationRequestV25.class ),
				Mockito.anyString(),
				Mockito.anyString(),
				Mockito.anyList(),
				Mockito.anyString())
		).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, GROUPS.get(0) +","+GROUPS.get(1) )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_DETAILS_HEADER, X_PAGOPA_PN_SRC_CH_DETAILS )
				.header(PnDeliveryRestConstants.NOTIFICATION_VERSION_HEADER, "1")
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
				PA_ID,
				notificationRequest,
				X_PAGOPA_PN_SRC_CH,
				X_PAGOPA_PN_SRC_CH_DETAILS,
				GROUPS,
				"1");

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}
	}

	private NewNotificationRequestV25 newNotificationRequest() {
		return NewNotificationRequestV25.builder()
				.group( "group" )
				.senderDenomination( "Comune di Milano" )
				.senderTaxId( "01199250158" )
				.paProtocolNumber( "protocol_number" )
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.recipients( Collections.singletonList( NotificationRecipientV24.builder()
								.recipientType( NotificationRecipientV24.RecipientTypeEnum.PF )
								.taxId( "LVLDAA85T50G702B" )
								.denomination( "Ada Lovelace" )
								.digitalDomicile( NotificationDigitalAddress.builder()
										.type( NotificationDigitalAddress.TypeEnum.PEC )
										.address( "address@domain.it" )
										.build() )
								.physicalAddress( NotificationPhysicalAddress.builder()
										.zip( "83100" )
										.municipality( "municipality" )
										.address( "address" )
										.build() )
								.payments( List.of(NotificationPaymentItem.builder()
										.build() ))
						.build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder()
								.digests( NotificationAttachmentDigests.builder()
										.sha256( FILE_SHA_256 )
										.build() )
								.contentType( "application/pdf" )
								.ref( NotificationAttachmentBodyRef.builder()
										.key( "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG" )
										.versionToken( "version_token" )
										.build() )
						.build() ) )
				.physicalCommunicationType( NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				.subject( "subject_length" )
				.taxonomyCode( "010101P" )
				.build();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postFailure(boolean isCheckEnabled) {
		// Given
		NewNotificationRequestV25 request = newNotificationRequest();
		Map<String,String> conflictMap = new HashMap<>();
		conflictMap.put( "noticeCode", "duplicatedNoticeCode" );
		PnIdConflictException exception = new PnIdConflictException( conflictMap );

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();

		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(request.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when( deliveryService.receiveNotification(
						Mockito.anyString(),
						any( NewNotificationRequestV25.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.isNull(),
						Mockito.isNull())
				).thenThrow( exception );

		//Then
		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.CONFLICT);

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(request.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}

	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postFailureWithTaxonomyCode(boolean isCheckEnabled) {
		// Given
		NewNotificationRequestV25 request = newNotificationRequest();

		PnInvalidInputException exception = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_TAXONOMYCODE, "Invalid taxonomyCode exception");
		// When
		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId("TEST_FAIL", PA_ID_DEFAULT)).thenThrow(exception);
		Mockito.when( deliveryService.receiveNotification(
				Mockito.anyString(),
				any( NewNotificationRequestV25.class ),
				Mockito.anyString(),
				Mockito.isNull(),
				Mockito.isNull(),
				Mockito.isNull())
		).thenThrow( exception );


		//Then
		webTestClient.post()
				.uri("/delivery/v2.5/requests")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST);

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(request.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}
	}

	@Test
	void postFailureBindExc() {
		// Given
		NewNotificationRequestV25 request = newNotificationRequest();
		request.setPaProtocolNumber( null );

		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postFailureRuntimeExc(boolean isCheckEnabled) {
		// Given
		NewNotificationRequestV25 request = newNotificationRequest();

		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();
		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(request.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when( deliveryService.receiveNotification( PA_ID, request, X_PAGOPA_PN_SRC_CH,null, Collections.emptyList(), null ) ).thenThrow( RuntimeException.class );

		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(request), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PA"  )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus()
				.is5xxServerError();

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(request.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void postSuccessWithAmount(boolean isCheckEnabled) throws PnIdConflictException {
		// Given
		NewNotificationRequestV25 notificationRequest = NewNotificationRequestV25.builder()
				.group( "group" )
				.senderDenomination( "Comune di Milano" )
				.senderTaxId( "01199250158" )
				.taxonomyCode("010101P")
				.paProtocolNumber( "protocol_number" )
				.amount(10000)
				.paymentExpirationDate("2023-10-22")
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.recipients( Collections.singletonList( NotificationRecipientV24.builder()
						.recipientType( NotificationRecipientV24.RecipientTypeEnum.PF )
						.taxId( "LVLDAA85T50G702B" )
						.denomination( "Ada Lovelace" )
						.digitalDomicile( NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address( "address@domain.it" )
								.build() )
						.physicalAddress( NotificationPhysicalAddress.builder()
								.zip( "83100" )
								.municipality( "municipality" )
								.address( "address" )
								.build() )
						.payments( List.of(NotificationPaymentItem.builder().build()) )
						.build() ) )
				.documents( Collections.singletonList( NotificationDocument.builder()
						.digests( NotificationAttachmentDigests.builder()
								.sha256( FILE_SHA_256 )
								.build() )
						.contentType( "application/pdf" )
						.ref( NotificationAttachmentBodyRef.builder()
								.key( "PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG" )
								.versionToken( "version_token" )
								.build() )
						.build() ) )
				.physicalCommunicationType( NewNotificationRequestV25.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
				.subject( "subject_length" )
				.build();

		NewNotificationResponse savedNotification = NewNotificationResponse.builder()
				.notificationRequestId( Base64Utils.encodeToString(IUN.getBytes(StandardCharsets.UTF_8)) )
				.paProtocolNumber( "protocol_number" ).build();

		// When
		TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();
		Mockito.when(cfg.isCheckTaxonomyCodeEnabled()).thenReturn(isCheckEnabled);
		Mockito.when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT)).thenReturn(Optional.of(taxonomyCodeDto));
		Mockito.when(deliveryService.receiveNotification(
						Mockito.anyString(),
						any( NewNotificationRequestV25.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull())
				).thenReturn( savedNotification );

		// Then
		webTestClient.post()
				.uri(DELIVERY_REQUESTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(notificationRequest), NewNotificationRequestV25.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.header(PnDeliveryRestConstants.SOURCE_CHANNEL_HEADER, X_PAGOPA_PN_SRC_CH)
				.exchange()
				.expectStatus().isAccepted();

		Mockito.verify( deliveryService ).receiveNotification(
						Mockito.anyString(),
						any( NewNotificationRequestV25.class ),
						Mockito.anyString(),
						Mockito.isNull(),
						Mockito.anyList(),
						Mockito.isNull());

		if (isCheckEnabled) {
			Mockito.verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(notificationRequest.getTaxonomyCode(), PA_ID_DEFAULT);
		} else {
			Mockito.verify(taxonomyCodeDaoDynamo, Mockito.never()).getTaxonomyCodeByKeyAndPaId(any(), any());
		}

	}

	@Test
	void postPresignedUploadNotSuccess() {
		// Given
		List<PreLoadRequest> requests = new ArrayList<>();
		requests.add( PreLoadRequest.builder()
				.sha256( FILE_SHA_256 )
				.build());
		PnInvalidInputException pn = mock(PnInvalidInputException.class);

		// When
		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( 0 );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreLoadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus().isBadRequest();

	}

	@Test
	void postPresignedUploadSuccess() {
		// Given
		List<PreLoadRequest> requests = new ArrayList<>();
		requests.add( PreLoadRequest.builder()
				.sha256( FILE_SHA_256 )
				.build());
		List<PreLoadResponse> responses = new ArrayList<>();
		responses.add( PreLoadResponse.builder()
				.secret( SECRET )
				.httpMethod( PreLoadResponse.HttpMethodEnum.PUT )
				.url( URL )
				.build());


		// When
		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );
		Mockito.when(attachmentService.preloadDocuments( Mockito.anyList() ))
				.thenReturn( responses );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreLoadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header(PnDeliveryRestConstants.UID_HEADER, "asdasd")
				.header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF"  )
				.header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "asdasd" )
				.exchange()
				.expectStatus().isOk();

		Mockito.verify( attachmentService ).preloadDocuments( requests );
	}

	@Test
	void postPresignedUploadFailure() {
		//GIven
		List<PreloadRequest> requests = new ArrayList<>();
		requests.add( PreloadRequest.builder()
				.build());
		requests.add( PreloadRequest.builder()
				.build());
		requests.add( PreloadRequest.builder()
				.build());

		Mockito.when(cfg.getNumberOfPresignedRequest()).thenReturn( MAX_NUMBER_REQUESTS );

		// Then
		webTestClient.post()
				.uri("/delivery/attachments/preload")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(requests), PreloadRequest.class)
				.header(PnDeliveryRestConstants.CX_ID_HEADER, PA_ID)
				.header( PnDeliveryRestConstants.UID_HEADER, "uid" )
				.header( PnDeliveryRestConstants.CX_TYPE_HEADER, "PF" )
				.exchange()
				.expectStatus().isBadRequest();

	}

}

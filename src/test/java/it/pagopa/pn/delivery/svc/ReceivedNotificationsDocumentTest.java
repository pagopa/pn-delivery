package it.pagopa.pn.delivery.svc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.NotificationRecipientAddressesDto;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDaoDynamo;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.rest.PnReceivedNotificationsController;
import it.pagopa.pn.delivery.rest.PnSentNotificationsController;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class ReceivedNotificationsDocumentTest {

	private static final String IUN = "IUN";
	private static final String PA_ID = "PA_ID";
	private static final int DOCUMENT_INDEX = 0;
	private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
	private static final String REDIRECT_URL = "http://redirectUrl";
	public static final String ATTACHMENT_BODY_STR = "Body";
	public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
	private static final String FILENAME = "filename.pdf";
	private static final String REQUEST_ID = "VkdLVi1VS0hOLVZJQ0otMjAyMjA1LVAtMQ==";
	private static final Integer AMOUNT = 10000;
	private static final String PAYMENT_EXPIRE_DATE = "2023-12-29";
	private static final String ABSTRACT = "abstract";
	private static final String IDEMPOTENCE_TOKEN = "idempotenceToken";

	@MockBean
	private NotificationRetrieverService svc;

	@MockBean
	private NotificationAttachmentService attachmentService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@MockBean
	private ModelMapperFactory modelMapperFactory;

	private NotificationEntityDao entityDao;

	private EntityToDtoNotificationMapper entity2DtoMapper;

	private PnDataVaultClientImpl pnDataVaultClient;

	private NotificationDao notificationDao;


	@BeforeEach
	void setup() {
		this.entityDao = Mockito.mock(NotificationEntityDao.class);
		this.entity2DtoMapper = Mockito.mock(EntityToDtoNotificationMapper.class);
		this.pnDataVaultClient = Mockito.mock(PnDataVaultClientImpl.class);
		this.notificationDao = new NotificationDaoDynamo(entityDao, null, null, entity2DtoMapper, pnDataVaultClient);
	}

	@Test
	void getReceivedNotificationDocumentIndex() {
		// When
		InternalNotification notifications = createDocumentsNotification();
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.anyList())).thenReturn(createBaseRecipientDto());
		Mockito.when( pnDataVaultClient.getNotificationAddressesByIun(Mockito.anyString())).thenReturn(new ArrayList<NotificationRecipientAddressesDto>());

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString());

		// Then

		List<NotificationDocument> documents = results.get().getDocuments();
		for (Integer i=0; i<documents.size(); i++) {
			assertEquals(documents.get(i).getDocIdx(), i.toString());			
		}
	}

	@Test
	void getReceivedNotificationDocumentIndex0() {
		// When
		InternalNotification notifications = createDocumentsNotification();
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.anyList())).thenReturn(createBaseRecipientDto());
		Mockito.when( pnDataVaultClient.getNotificationAddressesByIun(Mockito.anyString())).thenReturn(new ArrayList<NotificationRecipientAddressesDto>());

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString());

		// Then

		List<NotificationDocument> documents = results.get().getDocuments();
		assertEquals(documents.get(DOCUMENT_INDEX).getDocIdx(), DOCUMENT_INDEX+"");			
	}

	@Test
	@Disabled("Documents field is required")
	void getReceivedNotificationNoDocumentIndex() {
		// When
		InternalNotification notifications = createNoDocumentsNotification();
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.anyList())).thenReturn(createBaseRecipientDto());
		Mockito.when( pnDataVaultClient.getNotificationAddressesByIun(Mockito.anyString())).thenReturn(new ArrayList<NotificationRecipientAddressesDto>());

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString());

		// Then
		List<NotificationDocument> documents = results.get().getDocuments();
		assertNull(documents);
	}


	private List<BaseRecipientDto> createBaseRecipientDto() {
		ArrayList<BaseRecipientDto> result = new ArrayList<BaseRecipientDto>();
		BaseRecipientDto brd = new BaseRecipientDto();
		brd.setDenomination("test");
		result.add(brd);
		return result;
	}

	private Optional<NotificationEntity> createNotificationEntities() {
		NotificationEntity ne = NotificationEntity.builder()
				.iun( IUN )
				.requestId( REQUEST_ID )
				.idempotenceToken( IDEMPOTENCE_TOKEN )
				.sentAt( Instant.now() )
				.notificationFeePolicy( NewNotificationRequest.NotificationFeePolicyEnum.FLAT_RATE )
				.notificationAbstract( ABSTRACT )
				.paNotificationId( PA_PROTOCOL_NUMBER )
				.paymentExpirationDate( PAYMENT_EXPIRE_DATE )
				.senderPaId( PA_ID )
				.amount( AMOUNT )
				.build(); //new NotificationEntity(IUN, ABSTRACT, IDEMPOTENCE_TOKEN, PA_PROTOCOL_NUMBER , null, IUN, IUN, PA_ID, null, null, null, null, IUN, FILENAME, ATTACHMENT_BODY_STR, AMOUNT, PAYMENT_EXPIRE_DATE, REQUEST_ID);
		Optional<NotificationEntity> result = Optional.ofNullable(ne);
		return result;
	}

	private InternalNotification createDocumentsNotification() {
		return new InternalNotification(FullSentNotification.builder()
				.iun("IUN_DOCUMENT_01")
				.subject("Subject 01")
				.senderPaId( "pa_03" )
				.notificationStatus( NotificationStatus.ACCEPTED )
				.recipients( Collections.singletonList(
						NotificationRecipient.builder()
						.taxId("Codice Fiscale 01")
						.denomination("Denomination_TEST")
						.digitalDomicile(NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address("account@dominio.it")
								.build())
						.build()
						))
				.documents(Arrays.asList(
						NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder()
								.key("doc00")
								.versionToken("v01_doc00")
								.build()
								)
						.digests(NotificationAttachmentDigests.builder()
								.sha256("sha256_doc00")
								.build()
								)
						.build(),
						NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder()
								.key("doc01")
								.versionToken("v01_doc01")
								.build()
								)
						.digests(NotificationAttachmentDigests.builder()
								.sha256("sha256_doc01")
								.build()
								)
						.build(),
						NotificationDocument.builder()
						.ref( NotificationAttachmentBodyRef.builder()
								.key("doc02")
								.versionToken("v01_doc02")
								.build()
								)
						.digests(NotificationAttachmentDigests.builder()
								.sha256("sha256_doc01")
								.build()
								)
						.build()
						))
				.build(), Collections.emptyMap(), Collections.emptyList());
	}

	private InternalNotification createNoDocumentsNotification() {
		return new InternalNotification(FullSentNotification.builder()
				.iun("IUN_DOCUMENT_01")
				.subject("Subject 01")
				.senderPaId( "pa_03" )
				.notificationStatus( NotificationStatus.ACCEPTED )
				.recipients( Collections.singletonList(
						NotificationRecipient.builder()
						.taxId("Codice Fiscale 01")
						.denomination("Denomination_TEST")
						.digitalDomicile(NotificationDigitalAddress.builder()
								.type( NotificationDigitalAddress.TypeEnum.PEC )
								.address("account@dominio.it")
								.build())
						.build()
						))
				.build(), Collections.emptyMap(), Collections.emptyList());
	}

}

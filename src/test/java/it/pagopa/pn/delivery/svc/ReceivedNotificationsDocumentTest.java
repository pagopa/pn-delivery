package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationDaoDynamo;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationEntityDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.DocumentAttachmentEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationAttachmentBodyRefEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationAttachmentDigestsEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.rest.PnReceivedNotificationsController;
import it.pagopa.pn.delivery.rest.PnSentNotificationsController;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(controllers = {PnSentNotificationsController.class, PnReceivedNotificationsController.class})
class ReceivedNotificationsDocumentTest {

	private static final String IUN = "IUN";
	private static final String PA_ID = "PA_ID";
	private static final int DOCUMENT_INDEX = 0;
	private static final String PA_PROTOCOL_NUMBER = "paProtocolNumber";
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
	private NotificationQRService qrService;

	@MockBean
	private PnDeliveryConfigs cfg;

	@SpyBean
	private ModelMapper modelMapper;

	private NotificationEntityDao entityDao;

	private EntityToDtoNotificationMapper entity2DtoMapper;

	private PnDataVaultClientImpl pnDataVaultClient;

	private NotificationDao notificationDao;


	@BeforeEach
	void setup() {
		this.entityDao = Mockito.mock(NotificationEntityDao.class);
		this.entity2DtoMapper = Mockito.mock(EntityToDtoNotificationMapper.class);
		this.pnDataVaultClient = Mockito.mock(PnDataVaultClientImpl.class);
		this.notificationDao = new NotificationDaoDynamo(entityDao, null, null, null, entity2DtoMapper, pnDataVaultClient);
	}

	@Test
	void getReceivedNotificationDocumentIndex() {
		// When
		InternalNotification notifications = createDocumentsNotification();
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.anyList())).thenReturn(createBaseRecipientDto());
		Mockito.when( pnDataVaultClient.getNotificationAddressesByIun(Mockito.anyString())).thenReturn(new ArrayList<>());

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString(), false);

		// Then

		List<it.pagopa.pn.delivery.models.internal.notification.NotificationDocument> documents = results.get().getDocuments();
		for (int i = 0; i<documents.size(); i++) {
			assertEquals(documents.get(i).getDocIdx(), Integer.toString(i));
		}
	}

	@Test
	void getReceivedNotificationDocumentIndex0() {
		// When
		InternalNotification notifications = createDocumentsNotification();
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString(), false);

		// Then

		List<it.pagopa.pn.delivery.models.internal.notification.NotificationDocument> documents = results.get().getDocuments();
		assertEquals(documents.get(DOCUMENT_INDEX).getDocIdx(), DOCUMENT_INDEX+"");
	}

	@Test
	void getReceivedNotificationNoDocumentIndex() {
		// When
		InternalNotification notifications = createNoDocumentsNotification();
		notifications.setDocuments(List.of(
				NotificationDocument.builder()
						.docIdx("docIx")
						.contentType("application/pdf")
						.title("title")
						.build()
		));
		Optional<NotificationEntity> notificationEntities = createNotificationEntities();

		Mockito.when( entityDao.get(Mockito.any(Key.class))).thenReturn(notificationEntities);
		Mockito.when( entity2DtoMapper.entity2Dto(Mockito.any(NotificationEntity.class))).thenReturn(notifications);

		Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId(Mockito.anyList())).thenReturn(createBaseRecipientDto());
		Mockito.when( pnDataVaultClient.getNotificationAddressesByIun(Mockito.anyString())).thenReturn(new ArrayList<>());

		Optional<InternalNotification> results = notificationDao.getNotificationByIun(Mockito.anyString(), false);

		// Then
		List<it.pagopa.pn.delivery.models.internal.notification.NotificationDocument> documents = results.get().getDocuments();
		assertNotNull(documents);
	}


	private List<BaseRecipientDto> createBaseRecipientDto() {
		ArrayList<BaseRecipientDto> result = new ArrayList<>();
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
				.notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
				.notificationAbstract( ABSTRACT )
				.paNotificationId( PA_PROTOCOL_NUMBER )
				.paymentExpirationDate( PAYMENT_EXPIRE_DATE )
				.senderPaId( PA_ID )
				.amount( AMOUNT )
				.documents(List.of(DocumentAttachmentEntity.builder()
						.contentType("application/pdf")
						.digests(NotificationAttachmentDigestsEntity.builder()
								.sha256("Zsg9Nyzj13UPzkyaQlnA7wbgTfBaZmH02OVyiRjpydE=").build())
						.ref(NotificationAttachmentBodyRefEntity.builder().key("KEY").versionToken("version").build())
						.build()))
				.build(); //new NotificationEntity(IUN, ABSTRACT, IDEMPOTENCE_TOKEN, PA_PROTOCOL_NUMBER , null, IUN, IUN, PA_ID, null, null, null, null, IUN, FILENAME, ATTACHMENT_BODY_STR, AMOUNT, PAYMENT_EXPIRE_DATE, REQUEST_ID);
		return Optional.ofNullable(ne);
	}

	private InternalNotification createDocumentsNotification() {
		InternalNotification internalNotification = new InternalNotification();
		internalNotification.setIun("iun");
		internalNotification.setPaProtocolNumber("protocol_01");
		internalNotification.setSubject("Subject 01");
		internalNotification.setCancelledIun("IUN_05");
		internalNotification.setCancelledIun("IUN_00");
		internalNotification.setSenderPaId("PA_ID");
		internalNotification.setNotificationStatus(NotificationStatusV26.IN_VALIDATION);
		internalNotification.setDocuments(List.of(NotificationDocument.builder()
				.docIdx("doc")
				.title("title")
				.contentType("application/pdf")
				.digests(NotificationAttachmentDigests.builder()
						.sha256("Zsg9Nyzj13UPzkyaQlnA7wbgTfBaZmH02OVyiRjpydE=").build())
				.ref(NotificationAttachmentBodyRef.builder().key("key").versionToken("version").build())
				.build()));
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

	private InternalNotification createNoDocumentsNotification() {
		InternalNotification internalNotification = new InternalNotification();
		internalNotification.setIun("iun");
		internalNotification.setPaProtocolNumber("protocol_01");
		internalNotification.setSubject("Subject 01");
		internalNotification.setCancelledIun("IUN_05");
		internalNotification.setCancelledIun("IUN_00");
		internalNotification.setSenderPaId("PA_ID");
		internalNotification.setNotificationStatus(NotificationStatusV26.IN_VALIDATION);
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

}

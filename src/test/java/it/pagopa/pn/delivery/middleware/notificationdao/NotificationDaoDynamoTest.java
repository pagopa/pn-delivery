package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.internal.notification.MetadataAttachment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class NotificationDaoDynamoTest {

    private NotificationDaoDynamo dao;
    private EntityToDtoNotificationMapper entity2dto;
    private PnDataVaultClientImpl pnDataVaultClient;
    private NotificationEntityDao entityDao;

    private NotificationDelegationMetadataEntityDao delegationMetadataEntityDao;

    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
    private static final String VERSION_TOKEN = "VERSION_TOKEN";
    private static final String KEY = "KEY";

    @BeforeEach
    void setup() {
        DtoToEntityNotificationMapper dto2Entity = new DtoToEntityNotificationMapper();
        entity2dto = new EntityToDtoNotificationMapper();
        entityDao = new EntityDaoMock();
        NotificationMetadataEntityDao metadataEntityDao = new MetadataEntityDaoMock();
        pnDataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        delegationMetadataEntityDao = Mockito.mock( NotificationDelegationMetadataEntityDao.class );
        dao = new NotificationDaoDynamo( entityDao, metadataEntityDao, delegationMetadataEntityDao, dto2Entity, entity2dto, pnDataVaultClient);
    }

    @Test
    void getRequestId(){

        entityDao.get(Key.builder()
                .partitionValue( "senderId" + "##" + "paProtocolNumber" + "##" + "id" )
                .build());


        Optional<String> pageSearchTrunk = this.dao.getRequestId("senderId","paProtocolNumber","id");

        Assertions.assertNotNull(pageSearchTrunk);
    }

    @Test
    void insertSuccessWithoutPayments() throws PnIdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );
        notification.setPhysicalCommunicationType(FullSentNotificationV21.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER);

        // WHEN
        when( pnDataVaultClient.ensureRecipientByExternalId( any(it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        // THEN
        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setRecipientType( it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.PF );
        baseRecipientDto.setDenomination( "recipientDenomination" );
        baseRecipientDto.setInternalId( "opaqueTaxId" );
        baseRecipientDto.setTaxId( "recipientTaxId" );

        BaseRecipientDto baseRecipientDto1 = new BaseRecipientDto();
        baseRecipientDto1.setRecipientType( it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.PF );
        baseRecipientDto1.setDenomination( "recipientDenomination1" );
        baseRecipientDto1.setInternalId( "opaqueTaxId1" );
        baseRecipientDto1.setTaxId( "recipientTaxId1" );

        when( pnDataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( List.of(baseRecipientDto1, baseRecipientDto ) );

        NotificationRecipientAddressesDto notificationRecipientAddressesDto = new NotificationRecipientAddressesDto();
        notificationRecipientAddressesDto.setDenomination( "recipientDenomination" );
        notificationRecipientAddressesDto.setDigitalAddress( new AddressDto().value( "digitalAddress" ) );
        notificationRecipientAddressesDto.setPhysicalAddress( new AnalogDomicile()
                .address( "physicalAddress" )
                .addressDetails( "addressDetail" )
                .at( "at" )
                .municipality( "municipality" )
                .province( "province" )
                .cap( "cap" )
                .state( "state" ));

        NotificationRecipientAddressesDto notificationRecipientAddressesDto1 = new NotificationRecipientAddressesDto();
        notificationRecipientAddressesDto1.setDenomination( "recipientDenomination1" );
        notificationRecipientAddressesDto1.setDigitalAddress( new AddressDto().value( "digitalAddress1" ) );
        notificationRecipientAddressesDto1.setPhysicalAddress( new AnalogDomicile()
                .address( "physicalAddress1" )
                .addressDetails( "addressDetail1" )
                .at( "at1" )
                .municipality( "municipality1" )
                .province( "province1" )
                .cap( "cap1" )
                .state( "state1" ));
        when( pnDataVaultClient.getNotificationAddressesByIun( "IUN_01" ) ).thenReturn( List.of( notificationRecipientAddressesDto ,notificationRecipientAddressesDto1 ) );
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        // verifica ordine taxId destinatari
        Assertions.assertEquals(saved.get().getRecipients().get(0).getTaxId(), baseRecipientDto.getTaxId());
        // verifica ordine indirizzi
        assert notificationRecipientAddressesDto.getDigitalAddress() != null;
        Assertions.assertEquals( saved.get().getRecipients().get(0).getDigitalDomicile().getAddress(), notificationRecipientAddressesDto.getDigitalAddress().getValue() );
        //Assertions.assertEquals( notification, saved.get() );
    }



    @Test
    void insertFailForIunConflict() throws PnIdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        when( pnDataVaultClient.ensureRecipientByExternalId( any(it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );
        Executable todo = () -> this.dao.addNotification( newNotificationWithoutPayments() );

        // THEN
        Assertions.assertThrows( PnIdConflictException.class, todo );
    }


    @Test
    void insertSuccessWithPayments() throws PnIdConflictException {

        // GIVEN
        InternalNotification notification = newNotification( );

        // WHEN
        when( pnDataVaultClient.ensureRecipientByExternalId( any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        NotificationRecipientAddressesDto notificationRecipientAddressesDto = new NotificationRecipientAddressesDto();
        notificationRecipientAddressesDto.setDenomination( "recipientDenomination" );
        notificationRecipientAddressesDto.setDigitalAddress( new AddressDto().value( "digitalAddress" ) );
        notificationRecipientAddressesDto.setPhysicalAddress( new AnalogDomicile()
                .address( "physicalAddress" )
                .addressDetails( "addressDetail" )
                .at( "at" )
                .municipality( "municipality" )
                .province( "province" )
                .cap( "cap" )
                .state( "state" ));

        NotificationRecipientAddressesDto notificationRecipientAddressesDto1 = new NotificationRecipientAddressesDto();
        notificationRecipientAddressesDto1.setDenomination( "recipientDenomination1" );
        notificationRecipientAddressesDto1.setDigitalAddress( new AddressDto().value( "digitalAddress1" ) );
        notificationRecipientAddressesDto1.setPhysicalAddress( new AnalogDomicile()
                .address( "physicalAddress1" )
                .addressDetails( "addressDetail1" )
                .at( "at1" )
                .municipality( "municipality1" )
                .province( "province1" )
                .cap( "cap1" )
                .state( "state1" ));

        when( pnDataVaultClient.getNotificationAddressesByIun( "IUN_01" ) ).thenReturn( List.of( notificationRecipientAddressesDto ,notificationRecipientAddressesDto1 ) );
        // THEN
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        //Assertions.assertEquals( notification, saved.get() );
    }

    @Test
    void testWrongRecipientJson() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                //.recipientsJson(Collections.singletonMap(cf, "WRONG JSON"))
                //.recipientsOrder(Collections.singletonList(cf))
                .recipients( Collections.singletonList( NotificationRecipientEntity.builder()
                        .recipientType( RecipientTypeEntity.PF )
                        .build() ))
                .build();

        // WHEN
        Executable todo = () -> entity2dto.entity2Dto(entity);

        // THEN
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void regExpMatchTest() {

        Predicate<String> predicate = this.dao.buildRegexpPredicate("Test");
        //boolean b = Pattern.compile("^Test$").matcher("Subject Test").matches();

        Assertions.assertTrue(predicate.test("Test"));
        Assertions.assertFalse(predicate.test("Subject Test"));

        Predicate<String> predicate2 = this.dao.buildRegexpPredicate(".*Test");

        Assertions.assertTrue(predicate2.test("Test"));
        Assertions.assertTrue(predicate2.test("Subject Test"));

    }

    @Test
    void searchDelegatedForOneMonth() {
        InputSearchNotificationDelegatedDto dto = new InputSearchNotificationDelegatedDto();
        PageSearchTrunk<NotificationDelegationMetadataEntity> page = new PageSearchTrunk<>();
        page.setResults(Collections.singletonList(new NotificationDelegationMetadataEntity()));
        when(delegationMetadataEntityDao.searchForOneMonth(any(), any(), any(), anyInt(), any())).thenReturn(page);
        Assertions.assertDoesNotThrow(() -> {
            this.dao.searchDelegatedForOneMonth(dto, IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_IUN, "partitionValue", 1, null);
        });
    }

    @Test
    void searchForOneMonth() {
        InputSearchNotificationDto dto = new InputSearchNotificationDto();
        PageSearchTrunk<NotificationDelegationMetadataEntity> page = new PageSearchTrunk<>();
        page.setResults(Collections.singletonList(new NotificationDelegationMetadataEntity()));
        when(delegationMetadataEntityDao.searchForOneMonth(any(), any(), any(), anyInt(), any())).thenReturn(page);
        Assertions.assertDoesNotThrow(() -> {
            this.dao.searchForOneMonth(dto, "indexName", "partitionValue", 1, null);
        });
    }

    @Test
    void findByPk() {
        InputSearchNotificationDelegatedDto dto = new InputSearchNotificationDelegatedDto();
        Page<NotificationDelegationMetadataEntity> page = Page.create(Collections.singletonList(new NotificationDelegationMetadataEntity()), null);
        when(delegationMetadataEntityDao.searchExactNotification(any())).thenReturn(page);
        Assertions.assertDoesNotThrow(() -> {
            this.dao.searchByPk(dto);
        });
    }

    @Test
    void searchByIUN(){

        String iun = "IUN";
        String senderId = "sender-pa-id";

        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto();
        inputSearchNotificationDto.setIunMatch(iun);
        entityDao.put(NotificationEntity.builder()
                .iun(iun)
                .sentAt(Instant.now())
                .recipients(List.of(NotificationRecipientEntity.builder().recipientId("rec1").build()))
                .senderPaId(senderId)
                .build());


        PageSearchTrunk<NotificationMetadataEntity> pageSearchTrunk = this.dao.searchByIUN(inputSearchNotificationDto);

        Assertions.assertNotNull(pageSearchTrunk);
        Assertions.assertEquals(1, pageSearchTrunk.getResults().size());

    }


    @Test
    void searchByIUN_notfound(){

        String iun = "IUN";
        String senderId = "sender-pa-id";

        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto();
        inputSearchNotificationDto.setIunMatch(iun);
        entityDao.put(NotificationEntity.builder()
                .iun(iun+"other")
                .sentAt(Instant.now())
                .recipients(List.of(NotificationRecipientEntity.builder().recipientId("rec1").build()))
                .senderPaId(senderId)
                .build());


        PageSearchTrunk<NotificationMetadataEntity> pageSearchTrunk = this.dao.searchByIUN(inputSearchNotificationDto);

        Assertions.assertNotNull(pageSearchTrunk);
        Assertions.assertNull(pageSearchTrunk.getResults());

    }


    @Test
    void searchByIUN_mandateNotAllowedPA(){

        String iun = "IUN";
        String senderId = "sender-pa-id";

        InputSearchNotificationDto inputSearchNotificationDto = new InputSearchNotificationDto();
        inputSearchNotificationDto.setIunMatch(iun);
        inputSearchNotificationDto.setMandateAllowedPaIds(List.of("pa-allowed-id"));

        entityDao.put(NotificationEntity.builder()
                .iun(iun)
                .sentAt(Instant.now())
                .recipients(List.of(NotificationRecipientEntity.builder().recipientId("rec1").build()))
                .senderPaId(senderId)
                .build());


        PageSearchTrunk<NotificationMetadataEntity> pageSearchTrunk = this.dao.searchByIUN(inputSearchNotificationDto);

        Assertions.assertNotNull(pageSearchTrunk);
        Assertions.assertNull(pageSearchTrunk.getResults());

    }

    private static class EntityDaoMock implements NotificationEntityDao {

        private final Map<Key, NotificationEntity> storage = new ConcurrentHashMap<>();


        @Override
        public void put(NotificationEntity notificationEntity) {
            Key key = Key.builder()
                    .partitionValue( notificationEntity.getIun() )
                    .build();
            storage.put(key, notificationEntity);
        }

        @Override
        public void putIfAbsent(NotificationEntity notificationEntity) throws PnIdConflictException {
            Key key = Key.builder()
                    .partitionValue( notificationEntity.getIun() )
                    .build();
            NotificationEntity previous =  storage.putIfAbsent( key, notificationEntity );
            if (previous != null) {
                Map<String,String> keyValueConflicts = new HashMap<>();
                keyValueConflicts.put( "iun", previous.getIun() );
                throw new PnIdConflictException( keyValueConflicts );
            }
        }

        @Override
        public Optional<NotificationEntity> get(Key key) {
            NotificationEntity entity = storage.get( key );
            return Optional.ofNullable( entity );
        }

        @Override
        public void delete(Key key) {
            storage.remove( key );
        }
    }

    private static class MetadataEntityDaoMock implements NotificationMetadataEntityDao {

        @Override
        public void put(NotificationMetadataEntity notificationMetadataEntity) {

        }

        @Override
        public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) throws PnIdConflictException {

        }

        @Override
        public Optional<NotificationMetadataEntity> get(Key key) {
            return Optional.empty();
        }

        @Override
        public void delete(Key key) {

        }

        @Override
        public PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }

        @Override
        public PageSearchTrunk<NotificationMetadataEntity> searchByIun(InputSearchNotificationDto inputSearchNotificationDto, String partitionValue, String sentAt) {
            return new PageSearchTrunk<>(List.of(NotificationMetadataEntity.builder().build()), new ConcurrentHashMap<>());
        }
    }

    private static class DelegationMetadataEntityDaoMock implements NotificationDelegationMetadataEntityDao {
        @Override
        public void put(NotificationDelegationMetadataEntity value) {

        }

        @Override
        public void putIfAbsent(NotificationDelegationMetadataEntity value) throws PnIdConflictException {

        }

        @Override
        public Optional<NotificationDelegationMetadataEntity> get(Key key) {
            return Optional.empty();
        }

        @Override
        public void delete(Key key) {

        }

        @Override
        public PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth(InputSearchNotificationDelegatedDto searchDto, IndexNameAndPartitions.SearchIndexEnum indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }

        @Override
        public PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedByMandateId(String mandateId,
                                                                                                Set<String> groups,
                                                                                                int size,
                                                                                                PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }

        @Override
        public List<NotificationDelegationMetadataEntity> batchDeleteItems(List<NotificationDelegationMetadataEntity> deleteBatchItems) {
            return null;
        }

        @Override
        public List<NotificationDelegationMetadataEntity> batchPutItems(List<NotificationDelegationMetadataEntity> items) {
            return null;
        }

        @Override
        public Optional<NotificationDelegationMetadataEntity> deleteWithConditions(NotificationDelegationMetadataEntity entity) {
            return Optional.empty();
        }

        @Override
        public Page<NotificationDelegationMetadataEntity> searchExactNotification(InputSearchNotificationDelegatedDto searchDto) {
            return Page.create(Collections.emptyList(), null);
        }
    }

    private InternalNotification newNotificationWithoutPayments() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setVat(0);
        internalNotification.setPaFee(0);
        internalNotification.setPagoPaIntMode(NewNotificationRequestV21.PagoPaIntModeEnum.NONE);
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }

    private InternalNotification newNotification() {
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setPaFee(0);
        internalNotification.setVat(0);
        internalNotification.setPhysicalCommunicationType(FullSentNotificationV21.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER);
        internalNotification.setPagoPaIntMode(NewNotificationRequestV21.PagoPaIntModeEnum.NONE);
        internalNotification.setSentAt(OffsetDateTime.now());
        internalNotification.setIun("IUN_01");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId("PA_ID");
        internalNotification.setNotificationStatus(NotificationStatus.ACCEPTED);
        internalNotification.setNotificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE);
        internalNotification.setPaFee(0);
        internalNotification.setVat(0);
        internalNotification.setDocuments(List.of(NotificationDocument
                .builder()
                .digests(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests.builder()
                        .sha256(SHA256_BODY)
                        .build())
                .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder()
                        .key(KEY)
                        .versionToken(VERSION_TOKEN)
                        .build())
                .build()));
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .payments(List.of(NotificationPaymentInfo.builder()
                                .f24(it.pagopa.pn.delivery.models.internal.notification.F24Payment.builder()
                                        .title("title")
                                        .applyCost(false)
                                        .metadataAttachment(MetadataAttachment.builder()
                                                .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder().build())
                                                .contentType("application/json")
                                                .digests(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests.builder().build())
                                                .build())
                                        .build())
                                .pagoPa(it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment.builder()
                                        .applyCost(false)
                                        .noticeCode("302211675775915057")
                                        .creditorTaxId("77777777777")
                                        .attachment(MetadataAttachment.builder()
                                                .ref(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef.builder().build())
                                                .contentType("application/json")
                                                .digests(it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentDigests.builder().build())
                                                .build())
                                        .build())
                                .build())
                        )
                        .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                        .physicalAddress(it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress.builder()
                                .address("address")
                                .addressDetails("address")
                                .zip("zip")
                                .at("at")
                                .municipality("municipality")
                                .province("province")
                                .foreignState("foreignState")
                                .build())
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }
}

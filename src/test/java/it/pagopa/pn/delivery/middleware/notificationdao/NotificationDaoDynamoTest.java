package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.RecipientType;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class NotificationDaoDynamoTest {

    private NotificationDaoDynamo dao;
    private EntityToDtoNotificationMapper entity2dto;
    private PnDataVaultClientImpl pnDataVaultClient;
    private NotificationEntityDao entityDao;
    private NotificationMetadataEntityDao metadataEntityDao;

    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    public static final String ATTACHMENT_BODY_STR = "Body";
    public static final String SHA256_BODY = DigestUtils.sha256Hex(ATTACHMENT_BODY_STR);
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

    @BeforeEach
    void setup() {
        DtoToEntityNotificationMapper dto2Entity = new DtoToEntityNotificationMapper();
        entity2dto = new EntityToDtoNotificationMapper();
        entityDao = new EntityDaoMock();
        metadataEntityDao = new MetadataEntityDaoMock();
        NotificationDelegationMetadataEntityDao delegationMetadataEntityDao = new DelegationMetadataEntityDaoMock();
        pnDataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        dao = new NotificationDaoDynamo( entityDao, metadataEntityDao, delegationMetadataEntityDao, dto2Entity, entity2dto, pnDataVaultClient);
    }

    @Test
    void insertSuccessWithoutPayments() throws PnIdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        Mockito.when( pnDataVaultClient.ensureRecipientByExternalId( Mockito.any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        // THEN
        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setRecipientType( RecipientType.PF );
        baseRecipientDto.setDenomination( "recipientDenomination" );
        baseRecipientDto.setInternalId( "opaqueTaxId" );
        baseRecipientDto.setTaxId( "recipientTaxId" );

        BaseRecipientDto baseRecipientDto1 = new BaseRecipientDto();
        baseRecipientDto1.setRecipientType( RecipientType.PF );
        baseRecipientDto1.setDenomination( "recipientDenomination1" );
        baseRecipientDto1.setInternalId( "opaqueTaxId1" );
        baseRecipientDto1.setTaxId( "recipientTaxId1" );

        Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( List.of(baseRecipientDto1, baseRecipientDto ) );

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

        Mockito.when( pnDataVaultClient.getNotificationAddressesByIun( Mockito.anyString() ) ).thenReturn( List.of( notificationRecipientAddressesDto ,notificationRecipientAddressesDto1 ) );
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        // verifica ordine taxId destinatari
        Assertions.assertEquals(saved.get().getRecipients().get(0).getTaxId(), baseRecipientDto.getTaxId());
        // verifica ordine indirizzi
        Assertions.assertEquals( saved.get().getRecipients().get(0).getDigitalDomicile().getAddress(), notificationRecipientAddressesDto.getDigitalAddress().getValue() );
        //Assertions.assertEquals( notification, saved.get() );
    }



    @Test
    void insertFailForIunConflict() throws PnIdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        Mockito.when( pnDataVaultClient.ensureRecipientByExternalId( Mockito.any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
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
        Mockito.when( pnDataVaultClient.ensureRecipientByExternalId( Mockito.any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        // THEN
        // TODO da sistemare la getNotificationByIun
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
            return new PageSearchTrunk<NotificationMetadataEntity>(List.of(NotificationMetadataEntity.builder().build()), new ConcurrentHashMap<>());
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
    }

    private InternalNotification newNotificationWithoutPayments() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber( "protocol_01" )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .subject("Subject 01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.AR_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .group( "Group_1" )
                .senderPaId( "pa_02" )
                .recipientIds(Collections.singletonList("Codice Fiscale 01"))
                .sourceChannel(X_PAGOPA_PN_SRC_CH)
                .sentAt( OffsetDateTime.now() )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                                .physicalAddress( NotificationPhysicalAddress.builder()
                                        .address( "address" )
                                        .zip( "zip" )
                                        .municipality( "municipality" )
                                        .at( "at" )
                                        .addressDetails( "addressDetails" )
                                        .province( "province" )
                                        .foreignState( "foreignState" )
                                        .build() )
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("key_doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .contentType( "application/pdf" )
                                .build(),
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("key_doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .contentType( "application/pdf" )
                                .build()
                ))
                .build());
    }

    /*private Notification newNotificationWithPaymentsDeliveryMode( boolean withIuv ) {
        return newNotificationWithoutPayments().toBuilder()
                .payment( NotificationPaymentInfo.builder()
                        .iuv( withIuv ? "iuv01" : null )
                        .notificationFeePolicy( NotificationPaymentInfoFeePolicies.DELIVERY_MODE )
                        .f24( NotificationPaymentInfo.F24.builder()
                                .digital( NotificationAttachment.builder()
                                        .ref( NotificationAttachment.Ref.builder()
                                                .key("key_F24dig")
                                                .versionToken("v01_F24dig")
                                                .build()
                                        )
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("sha__F24dig")
                                                .build()
                                        )
                                        .build()
                                )
                                .analog( NotificationAttachment.builder()
                                        .ref( NotificationAttachment.Ref.builder()
                                                .key("key_F24anag")
                                                .versionToken("v01_F24anag")
                                                .build()
                                        )
                                        .digests( NotificationAttachment.Digests.builder()
                                                .sha256("sha__F24anag")
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .build();
    }*/

    private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890 )
                .notificationFeePolicy( NotificationFeePolicy.FLAT_RATE )
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId("pa_02")
                .notificationStatus(NotificationStatus.ACCEPTED)
                .sentAt( OffsetDateTime.parse( "2023-03-16T12:30:23.123Z" ) )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .internalId( "recipientInternalId" )
                                .payment( NotificationPaymentInfo.builder()
                                        .noticeCode( "noticeCode" )
                                        .noticeCodeAlternative( "noticeCodeAlternative" )
                                        .pagoPaForm( null )
                                        .build() )
                                .recipientType( NotificationRecipient.RecipientTypeEnum.PF )
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref(NotificationAttachmentBodyRef.builder()
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
                                .ref(NotificationAttachmentBodyRef.builder()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .recipientIds( Collections.singletonList("recipientId") )
                .sourceChannel( X_PAGOPA_PN_SRC_CH )
                .build()
        );
    }
}

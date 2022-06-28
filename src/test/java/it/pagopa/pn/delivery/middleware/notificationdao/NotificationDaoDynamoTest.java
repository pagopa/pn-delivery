package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.RecipientTypeEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class NotificationDaoDynamoTest {

    private NotificationDaoDynamo dao;
    private EntityToDtoNotificationMapper entity2dto;
    private ModelMapperFactory modelMapperFactory;
    private PnDataVaultClientImpl pnDataVaultClient;

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
        NotificationEntityDao entityDao = new EntityDaoMock();
        NotificationMetadataEntityDao metadataEntityDao = new MetadataEntityDaoMock();
        pnDataVaultClient = Mockito.mock( PnDataVaultClientImpl.class );
        dao = new NotificationDaoDynamo( entityDao, metadataEntityDao, dto2Entity, entity2dto, pnDataVaultClient);
    }

    @Test
    void insertSuccessWithoutPayments() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        ModelMapper addMapper = new ModelMapper();
        addMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        addMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );
        //Mockito.when( modelMapperFactory.createModelMapper( NotificationRecipient.class, NotificationRecipientEntity.class ) ).thenReturn( addMapper );

        ModelMapper getMapper = new ModelMapper();
        getMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class );
        //Mockito.when( modelMapperFactory.createModelMapper( NotificationRecipientEntity.class, NotificationRecipient.class ) ).thenReturn( getMapper );
        Mockito.when( pnDataVaultClient.ensureRecipientByExternalId( Mockito.any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        // THEN
        BaseRecipientDto baseRecipientDto = new BaseRecipientDto();
        baseRecipientDto.setRecipientType( RecipientType.PF );
        baseRecipientDto.setDenomination( "recipientDenomination" );
        baseRecipientDto.setInternalId( "opaqueTaxId" );
        baseRecipientDto.setTaxId( "recipientTaxId" );

        Mockito.when( pnDataVaultClient.getRecipientDenominationByInternalId( Mockito.anyList() ) ).thenReturn( Collections.singletonList( baseRecipientDto ) );

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

        Mockito.when( pnDataVaultClient.getNotificationAddressesByIun( Mockito.anyString() ) ).thenReturn( Collections.singletonList( notificationRecipientAddressesDto ) );
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        //Assertions.assertEquals( notification, saved.get() );
    }



    @Test
    void insertFailForIunConflict() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        ModelMapper addMapper = new ModelMapper();
        addMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        addMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                        .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );


        //Mockito.when( modelMapperFactory.createModelMapper( NotificationRecipient.class, NotificationRecipientEntity.class ) ).thenReturn( addMapper );
        Mockito.when( pnDataVaultClient.ensureRecipientByExternalId( Mockito.any(RecipientType.class), Mockito.anyString() ) ).thenReturn( "opaqueTaxId" );
        this.dao.addNotification( notification );

        // THEN
        Assertions.assertThrows( IdConflictException.class, () ->
                this.dao.addNotification( newNotificationWithoutPayments() )
        );
    }


    @Test
    void insertSuccessWithPaymentsFlat() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithPaymentsFlat( );

        // WHEN
        ModelMapper addMapper = new ModelMapper();
        addMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        addMapper.createTypeMap( NotificationRecipient.class, NotificationRecipientEntity.class )
                .addMapping( NotificationRecipient::getTaxId, NotificationRecipientEntity::setRecipientId );

        //Mockito.when( modelMapperFactory.createModelMapper( NotificationRecipient.class, NotificationRecipientEntity.class ) ).thenReturn( addMapper );

        ModelMapper getMapper = new ModelMapper();
        getMapper.createTypeMap( NotificationRecipientEntity.class, NotificationRecipient.class );
        //Mockito.when( modelMapperFactory.createModelMapper( NotificationRecipientEntity.class, NotificationRecipient.class ) ).thenReturn( getMapper );
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
        public void putIfAbsent(NotificationEntity notificationEntity) throws IdConflictException {
            Key key = Key.builder()
                    .partitionValue( notificationEntity.getIun() )
                    .build();
            NotificationEntity previous =  storage.putIfAbsent( key, notificationEntity );
            if (previous != null) {
                Map<String,String> keyValueConflicts = new HashMap<>();
                keyValueConflicts.put( "iun", previous.getIun() );
                throw new IdConflictException( keyValueConflicts );
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
        public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) throws IdConflictException {

        }

        @Override
        public Optional<NotificationMetadataEntity> get(Key key) {
            return Optional.empty();
        }

        @Override
        public void delete(Key key) {

        }

        @Override
        public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }
    }

    private InternalNotification newNotificationWithoutPayments() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber( "protocol_01" )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
                .subject("Subject 01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .group( "Group_1" )
                .senderPaId( "pa_02" )
                .sentAt( Date.from( Instant.now() ) )
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
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
                .build(), Collections.emptyMap(), Collections.singletonList("Codice Fiscale 01"));
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

    /*private Notification newNotificationWithPaymentsIuvOnly() {
        return newNotificationWithoutPayments().toBuilder()
                .payment( NotificationPaymentInfo.builder()
                        .iuv( "IUV_01" )
                        .build()
                )
                .build();
    }*/

    private InternalNotification newNotificationWithPaymentsFlat( ) {
        InternalNotification notification =  newNotificationWithoutPayments();
        for (NotificationRecipient recipient : notification.getRecipients() ) {
            recipient.payment( NotificationPaymentInfo.builder()
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
                    .build() );
        }
        return notification;
    }

}

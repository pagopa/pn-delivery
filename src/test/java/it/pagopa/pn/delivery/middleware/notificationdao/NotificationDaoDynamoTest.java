package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.databind.ObjectMapper;



import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import jnr.ffi.annotations.In;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class NotificationDaoDynamoTest {

    private NotificationDaoDynamo dao;
    private EntityToDtoNotificationMapper entity2dto;

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
        ObjectMapper objMapper = new ObjectMapper();
        DtoToEntityNotificationMapper dto2Entity = new DtoToEntityNotificationMapper(objMapper);
        entity2dto = new EntityToDtoNotificationMapper(objMapper);
        NotificationEntityDao entityDao = new EntityDaoMock();
        NotificationMetadataEntityDao metadataEntityDao = new MetadataEntityDaoMock();
        dao = new NotificationDaoDynamo( entityDao, metadataEntityDao, dto2Entity, entity2dto );
    }

    @Test
    void insertSuccessWithoutPayments() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        //Assertions.assertEquals( notification, saved.get() );
    }



    @Test
    void insertFailForIunConflict() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithoutPayments( );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Assertions.assertThrows( IdConflictException.class, () ->
                this.dao.addNotification( notification )
        );
    }


    @Test
    void insertSuccessWithPaymentsFlat() throws IdConflictException {

        // GIVEN
        InternalNotification notification = newNotificationWithPaymentsFlat( );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Optional<InternalNotification> saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertTrue( saved.isPresent() );
        //Assertions.assertEquals( notification, saved.get() );
    }

    @Test
    void testWrongRecipientJson() {
        // GIVEN
        String cf = "CodiceFiscale";
        NotificationEntity entity = NotificationEntity.builder()
                .recipientsJson(Collections.singletonMap(cf, "WRONG JSON"))
                .recipientsOrder(Collections.singletonList(cf))
                .build();

        // WHEN
        Executable todo = () -> entity2dto.entity2Dto(entity);

        // THEN
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void testWrongDocumentsLength() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds(Arrays.asList("v1", "v2"))
                .documentsDigestsSha256(Collections.singletonList("doc1"))
                .recipientsJson(Collections.emptyMap())
                .recipientsOrder(Collections.emptyList())
                .build();

        // WHEN
        Executable todo = () -> entity2dto.entity2Dto(entity);

        // THEN
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void testWrongF24Metadata1() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds(Collections.emptyList())
                .documentsDigestsSha256(Collections.emptyList())
                .recipientsJson(Collections.emptyMap())
                .recipientsOrder(Collections.emptyList())
                .f24DigitalVersionId(null)
                .f24DigitalDigestSha256("sha256")
                .build();

        // WHEN
        Executable todo = () -> entity2dto.entity2Dto(entity);

        // THEN
        Assertions.assertThrows(PnInternalException.class, todo);
    }

    @Test
    void testWrongF24Metadata2() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds(Collections.emptyList())
                .documentsDigestsSha256(Collections.emptyList())
                .recipientsJson(Collections.emptyMap())
                .recipientsOrder(Collections.emptyList())
                .f24DigitalVersionId("version")
                .f24DigitalDigestSha256(null)
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
                throw new IdConflictException( notificationEntity );
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
                .subject("Subject 01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .group( "Group_1" )
                .senderPaId( "pa_02" )
                .sentAt( Date.from( Instant.now() ) )
                .recipients( Collections.singletonList(NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type(NotificationDigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
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
                .build(), Collections.EMPTY_MAP);
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
        NotificationRecipient recipient = NotificationRecipient.builder()
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
                        //.iuv( "IUV_01" )
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
                        .build()
                ).build();
        return new InternalNotification( FullSentNotification.builder()
                .iun("IUN_01")
                .sentAt( Date.from(Instant.now()))
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .physicalCommunicationType( FullSentNotification.PhysicalCommunicationTypeEnum.SIMPLE_REGISTERED_LETTER )
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId(" pa_02")
                .recipients( Collections.singletonList( recipient ) )
                .documents(List.of(
                        NOTIFICATION_REFERRED_ATTACHMENT
                ))
                .notificationFeePolicy( FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE )
                .group( "Group_1" )
                .build(), Collections.EMPTY_MAP );
    }

}

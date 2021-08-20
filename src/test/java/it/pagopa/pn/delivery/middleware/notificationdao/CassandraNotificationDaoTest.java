package it.pagopa.pn.delivery.middleware.notificationdao;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CassandraNotificationDaoTest extends AbstractNotificationDaoTest {

    private DtoToEntityMapper dto2Entity;
    private EntityToDtoMapper entity2dto;

    @BeforeEach
    void instantiateDao() {
        ObjectMapper objMapper = new ObjectMapper();
        dto2Entity = new DtoToEntityMapper( objMapper );
        entity2dto = new EntityToDtoMapper( objMapper );

        KeyValueStore<String, NotificationEntity> entityDao = new EntityDaoMock();
        dao = new CassandraNotificationDao( entityDao, dto2Entity , entity2dto );
    }

    @Override
    @Test
    void testInsertOk() throws IdConflictException {
        super.testInsertOk();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsDeliveryMode() throws IdConflictException {
        super.testInsertOkWithPaymentsDeliveryMode();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsFlat() throws IdConflictException {
        super.testInsertOkWithPaymentsFlat();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsIuvOnly() throws IdConflictException {
        super.testInsertOkWithPaymentsIuvOnly();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsNoIuv() throws IdConflictException {
        super.testInsertOkWithPaymentsNoIuv();
    }

    @Override
    @Test
    void testInsertFail() throws IdConflictException {
        super.testInsertFail();
    }

    @Override
    @Test
    void testDelete() throws IdConflictException {
        super.testDelete();
    }

    @Test
    void testWrongRecipientJson() {
        // GIVEN
        String cf = "CodiceFiscale";
        NotificationEntity entity = NotificationEntity.builder()
                .recipientsJson( Collections.singletonMap( cf, "WRONG JSON"))
                .recipientsOrder(Collections.singletonList( cf ))
                .build();

        // WHEN
        Executable todo = () -> { entity2dto.entity2Dto( entity ); };

        // THEN
        Assertions.assertThrows( IllegalStateException.class,  todo );
    }

    @Test
    void testWrongDocumentsLength() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds(Arrays.asList("v1", "v2"))
                .documentsDigestsSha256(Collections.singletonList( "doc1" ))
                .recipientsJson( Collections.emptyMap() )
                .recipientsOrder( Collections.emptyList() )
                .build();

        // WHEN
        Executable todo = () -> { entity2dto.entity2Dto( entity ); };

        // THEN
        Assertions.assertThrows( IllegalStateException.class,  todo );
    }

    @Test
    void testWrongF24Metadata1() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds( Collections.emptyList() )
                .documentsDigestsSha256( Collections.emptyList() )
                .recipientsJson( Collections.emptyMap() )
                .recipientsOrder( Collections.emptyList() )
                .f24DigitalVersionId( null )
                .f24DigitalDigestSha256( "sha256" )
                .build();

        // WHEN
        Executable todo = () -> { entity2dto.entity2Dto( entity ); };

        // THEN
        Assertions.assertThrows( IllegalStateException.class,  todo );
    }

    @Test
    void testWrongF24Metadata2() {
        // GIVEN
        NotificationEntity entity = NotificationEntity.builder()
                .documentsVersionIds( Collections.emptyList() )
                .documentsDigestsSha256( Collections.emptyList() )
                .recipientsJson( Collections.emptyMap() )
                .recipientsOrder( Collections.emptyList() )
                .f24DigitalVersionId( "version" )
                .f24DigitalDigestSha256( null )
                .build();

        // WHEN
        Executable todo = () -> { entity2dto.entity2Dto( entity ); };

        // THEN
        Assertions.assertThrows( IllegalStateException.class,  todo );
    }


    private static class EntityDaoMock implements KeyValueStore<String, NotificationEntity> {

        private final Map<String, NotificationEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void put(NotificationEntity notificationEntity) {
            storage.put( notificationEntity.getIun(), notificationEntity );
        }

        @Override
        public void putIfAbsent(NotificationEntity notificationEntity) throws IdConflictException {
            NotificationEntity previous = storage.putIfAbsent(notificationEntity.getIun(), notificationEntity);
            if( previous != null ) {
                throw new IdConflictException( notificationEntity.getIun() );
            }
        }

        @Override
        public NotificationEntity get(String iun) {
            return storage.get( iun );
        }

        @Override
        public void delete(String iun) {
            storage.remove( iun );
        }
    }


}

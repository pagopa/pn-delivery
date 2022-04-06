package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityDaoDynamoTest {

    @BeforeEach
    void setup() {
        NotificationEntityDao entityDao = new NotificationEntityDaoDynamoTest.EntityDaoMock();
    }


    private static class EntityDaoMock implements NotificationEntityDao {

        private final Map<Key, NotificationEntity> storage = new ConcurrentHashMap<>();


        @Override
        public void put(NotificationEntity notificationEntity) {
            Key key = Key.builder()
                    .partitionValue(notificationEntity.getIun())
                    .build();
            storage.put(key, notificationEntity);
        }

        @Override
        public void putIfAbsent(NotificationEntity notificationEntity) throws IdConflictException {
            Key key = Key.builder()
                    .partitionValue(notificationEntity.getIun())
                    .build();
            NotificationEntity previous = storage.putIfAbsent(key, notificationEntity);
            if (previous != null) {
                throw new IdConflictException(notificationEntity);
            }
        }

        @Override
        public Optional<NotificationEntity> get(Key key) {
            NotificationEntity entity = storage.get(key);
            return Optional.ofNullable(entity);
        }

        @Override
        public void delete(Key key) {
            storage.remove(key);
        }
    }
}
package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDelegationMetadataEntityDaoDynamoTest {

    private NotificationDelegationMetadataEntityDao entityDao;

    @BeforeEach
    void setup() {
        entityDao = new NotificationDelegationMetadataEntityDaoDynamoTest.EntityDaoMock();
    }

    @Test
    void testInsertEntity() {
        Instant now = Instant.now();

        NotificationDelegationMetadataEntity entity = NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId("1")
                .sentAt(now)
                .build();
        entityDao.putIfAbsent(entity);

        Key key = Key.builder().partitionValue("1").sortValue(now.toString()).build();
        Optional<NotificationDelegationMetadataEntity> expected = entityDao.get(key);
        assertTrue(expected.isPresent());
        assertEquals(expected.get(), entity);
    }

    private static class EntityDaoMock implements NotificationDelegationMetadataEntityDao {

        private final Map<Key, NotificationDelegationMetadataEntity> storage = new ConcurrentHashMap<>();

        @Override
        public void put(NotificationDelegationMetadataEntity value) {

        }

        @Override
        public void putIfAbsent(NotificationDelegationMetadataEntity value) throws PnIdConflictException {
            Key key = Key.builder()
                    .partitionValue(value.getIunRecipientIdDelegateIdGroupId())
                    .sortValue(value.getSentAt().toString())
                    .build();
            storage.put(key, value);
        }

        @Override
        public Optional<NotificationDelegationMetadataEntity> get(Key key) {
            return Optional.of(storage.get(key));
        }

        @Override
        public void delete(Key key) {

        }

        @Override
        public PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth(InputSearchNotificationDelegatedDto searchDto, IndexNameAndPartitions.SearchIndexEnum indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }

        @Override
        public PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedByMandateId(String mandateId, int size, PnLastEvaluatedKey lastEvaluatedKey) {
            return null;
        }

        @Override
        public List<NotificationDelegationMetadataEntity> batchPutItems(List<NotificationDelegationMetadataEntity> items) {
            return null;
        }

        @Override
        public List<NotificationDelegationMetadataEntity> batchDeleteItems(List<NotificationDelegationMetadataEntity> deleteBatchItems) {
            return null;
        }

        @Override
        public Optional<NotificationDelegationMetadataEntity> deleteWithConditions(NotificationDelegationMetadataEntity entity) {
            return Optional.empty();
        }

        @Override
        public Page<NotificationDelegationMetadataEntity> searchExactNotification(InputSearchNotificationDelegatedDto searchDto) {
            return null;
        }
    }
}
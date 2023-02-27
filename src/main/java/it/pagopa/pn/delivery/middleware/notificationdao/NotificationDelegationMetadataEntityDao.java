package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Optional;

public interface NotificationDelegationMetadataEntityDao extends KeyValueStore<Key, NotificationDelegationMetadataEntity> {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth(InputSearchNotificationDelegatedDto searchDto,
                                                                            IndexNameAndPartitions.SearchIndexEnum indexName,
                                                                            String partitionValue,
                                                                            int size,
                                                                            PnLastEvaluatedKey lastEvaluatedKey);
    PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedByMandateId(String mandateId,
                                                                                     int size,
                                                                                     PnLastEvaluatedKey lastEvaluatedKey);

    List<NotificationDelegationMetadataEntity> batchDeleteItems(List<NotificationDelegationMetadataEntity> deleteBatchItems);

    List<NotificationDelegationMetadataEntity> batchPutItems(List<NotificationDelegationMetadataEntity> items);

    Optional<NotificationDelegationMetadataEntity> deleteWithConditions(NotificationDelegationMetadataEntity entity);
}

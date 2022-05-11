package it.pagopa.pn.delivery.middleware.notificationdao;




import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface NotificationMetadataEntityDao extends KeyValueStore<Key, NotificationMetadataEntity> {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );
}

package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class NotificationReworksDaoDynamo implements NotificationReworksDao {

    private final DynamoDbAsyncTable<NotificationReworksEntity> table;

    public NotificationReworksDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getNotificationReworksDao().getTableName(), TableSchema.fromBean(NotificationReworksEntity.class));
    }

    @Override
    public Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId) {
        Key hashKey = Key.builder().partitionValue(iun).sortValue(reworkId).build();
        return Mono.fromFuture(table.getItem(hashKey));
    }

    @Override
    public Mono<NotificationReworksEntity> putItem(NotificationReworksEntity entity) {
        return Mono.fromFuture(table.putItem(entity)).thenReturn(entity);
    }

    @Override
    public Mono<Page<NotificationReworksEntity>> findByIun(String iun, Map<String, AttributeValue> lastEvaluateKey, int limit) {
        Key key = Key.builder().partitionValue(iun).build();
        QueryConditional queryByHashKey = keyEqualTo(key);

        QueryEnhancedRequest.Builder queryEnhancedRequest = QueryEnhancedRequest
                .builder()
                .limit(limit)
                .queryConditional(queryByHashKey);

        if (!CollectionUtils.isEmpty(lastEvaluateKey)) {
            queryEnhancedRequest.exclusiveStartKey(lastEvaluateKey);
        }

        return Mono.from(table.query(queryEnhancedRequest.build()));
    }
}

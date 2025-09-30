package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnConflictException;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@Slf4j
public class NotificationReworksDaoDynamo extends BaseDao<NotificationReworksEntity> implements NotificationReworksDao {

    private final String ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM = "ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM";

    private final DynamoDbAsyncTable<NotificationReworksEntity> table;

    public NotificationReworksDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient,
                dynamoDbAsyncClient,
                cfg.getNotificationReworksDao().getTableName(),
                NotificationReworksEntity.class
        );
        this.table = dynamoDbEnhancedClient.table(cfg.getNotificationReworksDao().getTableName(), TableSchema.fromBean(NotificationReworksEntity.class));
    }

    public Mono<NotificationReworksEntity> findLatestByIun(String iun) {
        QueryConditional queryByHashKey = QueryConditional.keyEqualTo(Key.builder().partitionValue(iun).build());

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryByHashKey)
                .scanIndexForward(false)
                .limit(1)
                .build();

        return Mono.from(table.query(request))
                .flatMap(page -> Mono.justOrEmpty(page.items().stream().findFirst()));
    }

    @Override
    public Mono<NotificationReworksEntity> findByIunAndReworkId(String iun, String reworkId) {
        Key hashKey = Key.builder().partitionValue(iun).sortValue(reworkId).build();
        return Mono.fromFuture(table.getItem(hashKey));
    }

    @Override
    public Mono<NotificationReworksEntity> putIfAbsent(NotificationReworksEntity entity) {
        String expression = String.format("attribute_not_exists(%s) AND attribute_not_exists(%s)",
                NotificationReworksEntity.FIELD_REWORK_ID,
                NotificationReworksEntity.FIELD_IUN);

        return putIfAbsent(expression, entity)
                .onErrorMap(ConditionalCheckFailedException.class, ex -> {
                    log.error("Conditional check exception on NotificationReworksDaoDynamo putIfAbsent reworkId={} exmessage={}", entity.getReworkId(), ex.getMessage());
                    return new PnConflictException(ERROR_CODE_REWORK_ENTITY_DUPLICATED_ITEM, String.format("RequestId %s already exists", entity.getReworkId()));
                });
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

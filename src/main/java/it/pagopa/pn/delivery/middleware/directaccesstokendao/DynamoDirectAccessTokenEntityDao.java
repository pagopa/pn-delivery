package it.pagopa.pn.delivery.middleware.directaccesstokendao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Component
@Slf4j
public class DynamoDirectAccessTokenEntityDao  extends AbstractDynamoKeyValueStore<TokenEntity> {
    protected DynamoDirectAccessTokenEntityDao(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName(cfg), TableSchema.fromClass(TokenEntity.class)));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getDirectAccessTokenDao().getTableName();
    }

    @Override
    public void putIfAbsent(TokenEntity value) throws IdConflictException {
        String expression = "attribute_not_exists(" + TokenEntity.FIELD_TOKEN +")";

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<TokenEntity> request = PutItemEnhancedRequest.builder( TokenEntity.class )
                .item(value )
                .conditionExpression( conditionExpressionPut )
                .build();
        try {
            table.putItem(request);
        }catch (ConditionalCheckFailedException ex){
            log.error("Conditional check exception on PaperNotificationFailedEntityDaoDynamo putIfAbsent ", ex);
            throw new IdConflictException(value);
        }
    }
}

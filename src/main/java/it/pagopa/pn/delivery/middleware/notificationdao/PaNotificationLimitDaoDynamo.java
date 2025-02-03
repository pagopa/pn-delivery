package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.PaNotificationLimitEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;

@Component
@Slf4j
public class PaNotificationLimitDaoDynamo implements PaNotificationLimitDao {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Autowired
    public PaNotificationLimitDaoDynamo(DynamoDbClient dynamoDbClient, PnDeliveryConfigs pnDeliveryConfigs) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = pnDeliveryConfigs.getPaNotificationLimitDao().getTableName();
    }

    @Override
    public boolean decrementLimitIncrementDailyCounter(String pk, String dailyCounter) {
        log.debug("Decrementing limit and incrementing dailyCounter {} for pk: {}", dailyCounter, pk);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(getKey(pk))
                    .updateExpression("SET " + PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " = " +
                            PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " - :decrement, " + dailyCounter + " = " + dailyCounter + " + :increment")
                    .conditionExpression(PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " > :zero") //TODO Valutare se insereire la condition sull'esistenza
                    .expressionAttributeValues(Map.of(
                            ":decrement", AttributeValue.builder().n("1").build(),
                            ":increment", AttributeValue.builder().n("1").build(),
                            ":zero", AttributeValue.builder().n("0").build()
                    ))
                    .build();

            dynamoDbClient.updateItem(updateRequest);
            return true;

        } catch (ConditionalCheckFailedException ex) {
            log.error("Conditional check failed: {}", ex.getMessage(), ex);
            return false;
        } catch (DynamoDbException e) {
            log.error("Unable to update item in DynamoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void incrementLimitDecrementDailyCounter(String pk, String dailyCounter) {
        log.debug("Incrementing limit and decrementing dailyCounter {} for pk: {}", dailyCounter, pk);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(getKey(pk))
                    .updateExpression("SET " + PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " = " +
                            PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " + :increment, " + dailyCounter + " = " + dailyCounter + " - :decrement")
                    .conditionExpression("attribute_exists(" + PaNotificationLimitEntity.FIELD_PK + ")")
                    .expressionAttributeValues(Map.of(
                            ":increment", AttributeValue.builder().n("1").build(),
                            ":decrement", AttributeValue.builder().n("1").build()
                    ))
                    .build();

            dynamoDbClient.updateItem(updateRequest);

        } catch (ConditionalCheckFailedException ex) { //todo da non propagare?
            log.error("Conditional check failed: {}", ex.getMessage(), ex);
        } catch (DynamoDbException e) {
            log.error("Unable to update item in DynamoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean checkIfPaNotificationLimitExists(String pk) { //todo applicare consistent read -> il limite viene scritto sulla tabella diverso tempo prima, dunque non dovrebbe essere necessaria una consistent read
        log.debug("Checking if PaNotificationLimit exists for pk: {}", pk);
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(getKey(pk))
                .build();

        try {
            GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
            return getItemResponse.hasItem(); //Todo valutare se vale la pena ritornare l'item corrispondente e dunque effettuar eil check sul limite pirma di effettuare l'update (ci risparmia un pò di update)
        } catch (DynamoDbException e) {
            log.error("Unable to get item from DynamoDB: {}", e.getMessage(), e);
            throw new RuntimeException("GetItem failed: " + e.getMessage(), e);
        }
    }

    private Map<String, AttributeValue> getKey(String pk) {
        return Map.of(PaNotificationLimitEntity.FIELD_PK, AttributeValue.builder().s(pk).build());
    }
}
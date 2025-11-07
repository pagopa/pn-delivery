package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.PaNotificationLimitEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.OffsetDateTime;
import java.util.Map;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION;
import static it.pagopa.pn.delivery.middleware.notificationdao.utils.NotificationLimitUtils.createDailyCounter;
import static it.pagopa.pn.delivery.middleware.notificationdao.utils.NotificationLimitUtils.createPrimaryKey;

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
    public boolean decrementLimitIncrementDailyCounter(String paId, OffsetDateTime sentAt) {
        String pk = createPrimaryKey(paId, sentAt);
        log.info("Decrementing limit and incrementing dailyCounter for pk: {}", pk);
        String dailyCounter = createDailyCounter(sentAt);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(getFieldPk(pk))
                    .updateExpression("ADD " + PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " :decrement, " + dailyCounter + " :increment")
                    .conditionExpression(PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " > :zero and attribute_exists(" + PaNotificationLimitEntity.FIELD_PK + ")")
                    .expressionAttributeValues(Map.of(
                            ":decrement", AttributeValue.builder().n("-1").build(),
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
            throw new PnInternalException("Update failed: " + e.getMessage(), ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION);
        }
    }

    @Override
    public void incrementLimitDecrementDailyCounter(String paId, OffsetDateTime sentAt) {
        String pk = createPrimaryKey(paId, sentAt);
        log.info("Incrementing limit and decrementing dailyCounter for pk: {}", pk);
        String dailyCounter = createDailyCounter(sentAt);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(getFieldPk(pk))
                    .updateExpression("ADD " + PaNotificationLimitEntity.FIELD_RESIDUAL_LIMIT + " :increment, " + dailyCounter + " :decrement")
                    .conditionExpression("attribute_exists(" + PaNotificationLimitEntity.FIELD_PK + ")")
                    .expressionAttributeValues(Map.of(
                            ":increment", AttributeValue.builder().n("1").build(),
                            ":decrement", AttributeValue.builder().n("-1").build()
                    ))
                    .build();

            dynamoDbClient.updateItem(updateRequest);

        } catch (ConditionalCheckFailedException ex) {
            log.error("Conditional check failed: {}", ex.getMessage(), ex);
        } catch (DynamoDbException e) {
            log.error("Unable to update item in DynamoDB: {}", e.getMessage(), e);
            throw new PnInternalException("Update failed: " + e.getMessage(), ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION);
        }
    }

    @Override
    public boolean checkIfPaNotificationLimitExists(String paId, OffsetDateTime sentAt) {
        String pk = createPrimaryKey(paId, sentAt);
        log.info("Checking if paNotificationLimit exists for pk: {}", pk);
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(getFieldPk(pk))
                .build();

        try {
            GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
            return getItemResponse.hasItem();
        } catch (DynamoDbException e) {
            log.error("Unable to get item from DynamoDB: {}", e.getMessage(), e);
            throw new PnInternalException("GetItem failed: " + e.getMessage(), ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION);
        }
    }

    private Map<String, AttributeValue> getFieldPk(String pk) {
        return Map.of(PaNotificationLimitEntity.FIELD_PK, AttributeValue.builder().s(pk).build());
    }

}
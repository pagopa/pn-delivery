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

@Component
@Slf4j
public class PaNotificationLimitDaoDynamo implements PaNotificationLimitDao {
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String DAILY_COUNTER = "dailyCounter";

    @Autowired
    public PaNotificationLimitDaoDynamo(DynamoDbClient dynamoDbClient, PnDeliveryConfigs pnDeliveryConfigs) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = pnDeliveryConfigs.getPaNotificationLimitDao().getTableName();
    }

    @Override
    public boolean decrementLimitIncrementDailyCounter(String paId, OffsetDateTime sentAt) {
        log.info("Decrementing limit and incrementing dailyCounter for paId: {} sentAt: {}", paId, sentAt);
        String dailyCounter = createDailyCounter(sentAt);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(createPrimaryKey(paId, sentAt))
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
        log.info("Incrementing limit and decrementing dailyCounter for paId: {} sentAt: {}", paId, sentAt);
        String dailyCounter = createDailyCounter(sentAt);
        try {
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(createPrimaryKey(paId, sentAt))
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
        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(tableName)
                .key(createPrimaryKey(paId, sentAt))
                .build();

        try {
            GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
            return getItemResponse.hasItem();
        } catch (DynamoDbException e) {
            log.error("Unable to get item from DynamoDB: {}", e.getMessage(), e);
            throw new PnInternalException("GetItem failed: " + e.getMessage(), ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION);
        }
    }

    private Map<String, AttributeValue> createPrimaryKey(String paId, OffsetDateTime sentAt) {
        String pk = paId + "##" + sentAt.getYear() + "##" + String.format("%02d", sentAt.getMonthValue());
        log.info("Creating primary key: {}", pk);
        return Map.of(PaNotificationLimitEntity.FIELD_PK, AttributeValue.builder().s(pk).build());
    }

    private String createDailyCounter(OffsetDateTime sentAt) {
        String dailyCounter = DAILY_COUNTER + String.format("%02d", sentAt.getDayOfMonth());
        log.info("Creating daily counter: {}", dailyCounter);
        return dailyCounter;
    }

}
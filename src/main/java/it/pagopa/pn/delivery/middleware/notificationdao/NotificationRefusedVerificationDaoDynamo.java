package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRefusedVerificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION;

@Component
@Slf4j
public class NotificationRefusedVerificationDaoDynamo implements NotificationRefusedVerificationDao {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<NotificationRefusedVerificationEntity> dynamoDbTable;

    @Autowired
    public NotificationRefusedVerificationDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.dynamoDbTable = dynamoDbEnhancedClient.table(tableName(cfg), TableSchema.fromClass(NotificationRefusedVerificationEntity.class));
    }

    private static String tableName(PnDeliveryConfigs cfg) {
        return cfg.getNotificationRefusedVerificationDao().getTableName();
    }

    @Override
    public boolean putNotificationRefusedVerification(String pk) {
        log.info("Updating NotificationRefusedVerification for pk: {}", pk);
        try {
            Expression condition = Expression.builder()
                    .expression("attribute_not_exists(" + NotificationRefusedVerificationEntity.FIELD_PK + ")")
                    .build();
            PutItemEnhancedRequest<NotificationRefusedVerificationEntity> putItemEnhancedRequest = PutItemEnhancedRequest
                    .builder(NotificationRefusedVerificationEntity.class)
                    .conditionExpression(condition)
                    .item(getNotificationRefusedVerificationEntity(pk))
                    .build();

            dynamoDbTable.putItem(putItemEnhancedRequest);
            return true;
        } catch (ConditionalCheckFailedException ex) {
            log.error("Conditional check failed: {}", ex.getMessage(), ex);
            return false;
        } catch (DynamoDbException e) {
            log.error("Unable to update item in DynamoDB: {}", e.getMessage(), e);
            throw new PnInternalException("Update failed: " + e.getMessage(), ERROR_CODE_DELIVERY_DYNAMO_EXCEPTION);
        }
    }

    private NotificationRefusedVerificationEntity getNotificationRefusedVerificationEntity(String pk) {
        return NotificationRefusedVerificationEntity.builder()
                .pk(pk)
                .ttl(Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond())
                .build();
    }
}

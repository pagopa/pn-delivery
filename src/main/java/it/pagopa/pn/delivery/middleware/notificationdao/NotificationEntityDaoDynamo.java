package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Component
@Slf4j
public class NotificationEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationEntity> implements NotificationEntityDao<Key, NotificationEntity> {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<NotificationEntity> dynamoDbtable;

    public NotificationEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.dynamoDbtable = dynamoDbEnhancedClient.table(tableName( cfg), TableSchema.fromClass(NotificationEntity.class));

    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationDao().getTableName();
    }

    @Override
    public void putIfAbsent(NotificationEntity notificationEntity) throws IdConflictException {
        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(iun)")
                .build();

        PutItemEnhancedRequest<NotificationEntity> request1 = PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( notificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build();

        NotificationEntity controlNotificationEntity = NotificationEntity.builder()
                .iun( getControlIun(notificationEntity) )
                .build();

        PutItemEnhancedRequest<NotificationEntity> request2 = PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( controlNotificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build();

        TransactWriteItemsEnhancedRequest enhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem( dynamoDbtable, request1 )
                .addPutItem( dynamoDbtable, request2 )
                .build();

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            log.error("Unable to insert notification={}", notificationEntity ,ex );
            throw new IdConflictException( notificationEntity );
        }
    }

    @NotNull
    private String getControlIun(NotificationEntity notificationEntity) {
        return notificationEntity.getSenderPaId()
                + "##" + notificationEntity.getPaNotificationId()
                + "##" + notificationEntity.getCancelledIun();
    }
}

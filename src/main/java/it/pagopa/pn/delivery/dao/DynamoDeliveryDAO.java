package it.pagopa.pn.delivery.dao;


import it.pagopa.pn.delivery.model.notification.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.concurrent.CompletableFuture;

@Component
public class DynamoDeliveryDAO implements DeliveryDAO {

    private static final String NOTIFICATIONS_TABLE_NAME = "Notifications";

    private final DynamoDbEnhancedAsyncClient dynamoAsyncClient;

    public DynamoDeliveryDAO(
            DynamoDbEnhancedAsyncClient dynamoAsyncClient
        ) {
        this.dynamoAsyncClient = dynamoAsyncClient;
    }

    private TableSchema<Notification> tableSchema = TableSchema.fromClass(Notification.class);

    @Override
    public CompletableFuture<Void> addNotification(Notification notification) {
        PutItemEnhancedRequest req = PutItemEnhancedRequest.builder(Notification.class)
                .conditionExpression( Expression.builder()
                        .expression("iun <> :iun")
                        .putExpressionValue(":iun", AttributeValue.builder().s( notification.getIun() ).build() )
                        .build())
                .item( notification )
                .build();
        return dynamoAsyncClient.table( NOTIFICATIONS_TABLE_NAME, tableSchema )
                .putItem( req );
    }
}

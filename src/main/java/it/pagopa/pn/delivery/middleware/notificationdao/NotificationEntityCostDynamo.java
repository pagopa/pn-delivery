package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.Optional;

@Component
@Slf4j
public class NotificationEntityCostDynamo extends AbstractDynamoKeyValueStore<NotificationCostEntity> implements NotificationCostEntityDao {

    protected NotificationEntityCostDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationCostEntity.class)));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationCostDao().getTableName();
    }

    @Override
    public Optional<InternalNotificationCost> getNotificationByPaymentInfo(String paTaxId, String noticeCode) {

        Key key = Key.builder()
                .partitionValue( paTaxId+"##"+noticeCode )
                .build();

        NotificationCostEntity notificationCostEntity = table.getItem( key );
        if ( notificationCostEntity != null ){
            return Optional.of(InternalNotificationCost.builder()
                    .creditorTaxIdNoticeCode( notificationCostEntity.getCreditorTaxIdNoticeCode() )
                    .iun( notificationCostEntity.getIun() )
                    .recipientIdx( notificationCostEntity.getRecipientIdx() )
                    .recipientType (  notificationCostEntity.getRecipientType() )
                    .build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void putIfAbsent(NotificationCostEntity notificationCostEntity) throws PnIdConflictException {
        PutItemEnhancedRequest<NotificationCostEntity> request = PutItemEnhancedRequest.
                builder(NotificationCostEntity.class)
                .item( notificationCostEntity )
                .build();
        table.putItem( request );
    }

    @Override
    public void deleteItem(NotificationCostEntity notificationCostEntity) {
        table.deleteItem(notificationCostEntity);
    }
}

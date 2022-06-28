package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.models.NotificationCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Component
@Slf4j
public class NotificationEnitytCostDynamo extends AbstractDynamoKeyValueStore<NotificationCostEntity> implements NotificationCostEntityDao {

    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    protected NotificationEnitytCostDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationCostEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationCostDao().getTableName();
    }

    @Override
    public Optional<NotificationCost> getNotificationByPaymentInfo(String paTaxId, String noticeCode) {

        Key key = Key.builder()
                .partitionValue( paTaxId+"##"+noticeCode )
                .build();

        NotificationCostEntity notificationCostEntity = table.getItem( key );
        if ( notificationCostEntity != null ){
            return Optional.of(NotificationCost.builder()
                    .creditorTaxId_noticeCode( notificationCostEntity.getCreditorTaxId_noticeCode() )
                    .iun( notificationCostEntity.getIun() )
                    .recipientIdx( notificationCostEntity.getRecipientIdx() )
                    .build());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void putIfAbsent(NotificationCostEntity notificationCostEntity) throws IdConflictException {
        throw new UnsupportedOperationException();
    }
}

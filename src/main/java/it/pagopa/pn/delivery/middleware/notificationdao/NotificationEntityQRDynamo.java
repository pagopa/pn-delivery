package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.notification.NotificationRecipientType;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class NotificationEntityQRDynamo extends AbstractDynamoKeyValueStore<NotificationQREntity> implements NotificationQREntityDao {
    protected NotificationEntityQRDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationQREntity.class)));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationQRDao().getTableName();
    }

    @Override
    public Optional<InternalNotificationQR> getNotificationByQR(String aarQRCode) {
        Key key = Key.builder()
                .partitionValue( aarQRCode )
                .build();

        NotificationQREntity notificationQREntity = table.getItem( key );
        if ( Objects.nonNull(notificationQREntity) ) {
            return Optional.of( InternalNotificationQR.builder()
                            .iun( notificationQREntity.getIun() )
                            .aarQRCodeValue( notificationQREntity.getAarQRCodeValue() )
                            .recipientInternalId( notificationQREntity.getRecipientId() )
                            .recipientType( NotificationRecipientType.valueOf( notificationQREntity.getRecipientType().getValue() ))
                    .build() );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void putIfAbsent(NotificationQREntity notificationQREntity) throws PnIdConflictException {
        throw new UnsupportedOperationException();
    }
}

package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.List;

@Slf4j
@Component
public class NotificationDelegationMetadataEntityDaoDynamo
        extends AbstractDynamoKeyValueStore<NotificationDelegationMetadataEntity>
        implements NotificationDelegationMetadataEntityDao {

    protected NotificationDelegationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                                            PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName(cfg), TableSchema.fromClass(NotificationDelegationMetadataEntity.class)));
    }

    private static String tableName(PnDeliveryConfigs cfg) {
        return cfg.getNotificationDelegationMetadataDao().getTableName();
    }

    @Override
    public void putIfAbsent(NotificationDelegationMetadataEntity entity) throws PnIdConflictException {
        PutItemEnhancedRequest<NotificationDelegationMetadataEntity> request = PutItemEnhancedRequest
                .builder(NotificationDelegationMetadataEntity.class)
                .item(entity)
                .build();
        table.putItem(request);
    }

    @Override
    public PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth() {
        return null;
    }

    @Override
    public PageSearchTrunk<NotificationDelegationMetadataEntity> searchByIun(InputSearchNotificationDto inputSearchNotificationDto,
                                                                             String pk,
                                                                             String sk) {
        log.debug("START search notification delegation for single IUN - {} {}", pk, sk);

        GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                .key(k -> k.partitionValue(pk).sortValue(sk).build())
                .build();

        log.debug("START query execution");
        NotificationDelegationMetadataEntity entity = table.getItem(request);

        // TODO capire se ha senso applicare i filtri

        PageSearchTrunk<NotificationDelegationMetadataEntity> result = new PageSearchTrunk<>();
        result.setResults(List.of(entity));
        log.debug("END query execution");
        return result;
    }
}

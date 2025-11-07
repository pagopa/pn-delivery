package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.TaxonomyCodeEntity;
import it.pagopa.pn.delivery.models.TaxonomyCodeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Optional;

@Component
@Slf4j
public class TaxonomyCodeDaoDynamo implements TaxonomyCodeDao {
    private final DynamoDbTable<TaxonomyCodeEntity> taxonomyCodeTable;

    @Autowired
    public TaxonomyCodeDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        this.taxonomyCodeTable = dynamoDbEnhancedClient.table(tableName(cfg), TableSchema.fromClass(TaxonomyCodeEntity.class));
    }

    private static String tableName(PnDeliveryConfigs cfg) {
        return cfg.getTaxonomyCodeDao().getTableName();
    }

    @Override
    public Optional<TaxonomyCodeDto> getTaxonomyCodeByKeyAndPaId(String key, String paId) {
        if (key == null || key.isEmpty() || paId == null || paId.isEmpty()) {
            throw new IllegalArgumentException("Key and paId cannot be null or empty");
        }

        try {
            Key dynamoKey = Key.builder()
                               .partitionValue(key)
                               .sortValue(paId)
                               .build();

            Optional<TaxonomyCodeEntity> result = Optional.ofNullable(taxonomyCodeTable.getItem(r -> r.key(dynamoKey)));

            return result.map(taxonomyCodeEntity -> TaxonomyCodeDto.builder()
                    .key(taxonomyCodeEntity.getKey())
                    .paId(taxonomyCodeEntity.getPaId())
                    .build());

        } catch (DynamoDbException e) {
            log.error("Unable to get item from DynamoDB: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME;

@Slf4j
@Component
public class NotificationDelegationMetadataEntityDaoDynamo
        extends AbstractDynamoKeyValueStore<NotificationDelegationMetadataEntity>
        implements NotificationDelegationMetadataEntityDao {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    protected NotificationDelegationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                                            PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName(cfg), TableSchema.fromClass(NotificationDelegationMetadataEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
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
    public PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedByMandateId(String mandateId,
                                                                                            int size,
                                                                                            PnLastEvaluatedKey lastEvaluatedKey) {
        log.debug("START search by mandateId");

        Key key = Key.builder().partitionValue(mandateId).build();
        log.debug("key building done pk={}", key.partitionKeyValue());

        QueryConditional equalToConditional = QueryConditional.keyEqualTo(key);

        DynamoDbIndex<NotificationDelegationMetadataEntity> index = table.index(NotificationDelegationMetadataEntity.FIELD_MANDATE_ID);

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional(equalToConditional)
                .limit(size);

        if (lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty()) {
            requestBuilder.exclusiveStartKey(lastEvaluatedKey.getInternalLastEvaluatedKey());
        }

        log.debug("START query execution");
        SdkIterable<Page<NotificationDelegationMetadataEntity>> pages = index.query(requestBuilder.build());
        log.debug("END query execution");

        Page<NotificationDelegationMetadataEntity> page = pages.iterator().next();

        PageSearchTrunk<NotificationDelegationMetadataEntity> response = new PageSearchTrunk<>();
        response.setResults(page.items());
        response.setLastEvaluatedKey(page.lastEvaluatedKey());
        log.debug("END search by mandateId");

        return response;
    }


    @Override
    public PageSearchTrunk<NotificationDelegationMetadataEntity> searchForOneMonth(InputSearchNotificationDelegatedDto searchDto,
                                                                                   IndexNameAndPartitions.SearchIndexEnum indexName,
                                                                                   String partitionValue,
                                                                                   int size,
                                                                                   PnLastEvaluatedKey lastEvaluatedKey) {
        log.debug("START search for one month");
        Instant startDate = searchDto.getStartDate();
        Instant endDate = searchDto.getEndDate();

        Key.Builder builder = Key.builder().partitionValue(partitionValue);
        Key key1 = builder.sortValue(startDate.toString()).build();
        Key key2 = builder.sortValue(endDate.toString()).build();
        log.debug("key building done pk={} start-sk={} end-sk={}", key1.partitionKeyValue(), key1.sortKeyValue(), key2.sortKeyValue());

        QueryConditional betweenConditional = QueryConditional.sortBetween(key1, key2);

        DynamoDbIndex<NotificationDelegationMetadataEntity> index = table.index(indexName.getValue());

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional(betweenConditional)
                .limit(size)
                .scanIndexForward(false);

        addFilterExpression(searchDto, requestBuilder);

        if (lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty()) {
            String attributeName = retrieveAttributeName(indexName);
            if (lastEvaluatedKey.getInternalLastEvaluatedKey().get(attributeName).s().equals(partitionValue)) {
                requestBuilder.exclusiveStartKey(lastEvaluatedKey.getInternalLastEvaluatedKey());
            }
        }

        log.debug("START query execution");
        SdkIterable<Page<NotificationDelegationMetadataEntity>> pages = index.query(requestBuilder.build());
        log.debug("END query execution");

        Page<NotificationDelegationMetadataEntity> page = pages.iterator().next();

        PageSearchTrunk<NotificationDelegationMetadataEntity> response = new PageSearchTrunk<>();
        response.setResults(page.items());
        response.setLastEvaluatedKey(page.lastEvaluatedKey());
        log.debug("END search for one month");
        return response;
    }

    private void addFilterExpression(InputSearchNotificationDelegatedDto searchDto,
                                     QueryEnhancedRequest.Builder requestBuilder) {
        Expression.Builder filterExpressionBuilder = Expression.builder();
        StringBuilder expressionBuilder = new StringBuilder();

        addStatusFilterExpression(searchDto.getStatuses(), filterExpressionBuilder, expressionBuilder);
        addSenderFilterExpression(searchDto.getSenderId(), filterExpressionBuilder, expressionBuilder);
        addReceiverFilterExpression(searchDto.getReceiverId(), filterExpressionBuilder, expressionBuilder);

        requestBuilder.filterExpression(filterExpressionBuilder
                .expression(expressionBuilder.length() > 0 ? expressionBuilder.toString() : null)
                .build());
    }

    private void addStatusFilterExpression(List<NotificationStatus> status,
                                           Expression.Builder filterExpressionBuilder,
                                           StringBuilder expressionBuilder) {
        if (CollectionUtils.isEmpty(status)) {
            log.debug("status filter is empty - skip add status filter expression");
            return;
        }
        addEventuallyAnd(expressionBuilder);
        expressionBuilder.append(NotificationDelegationMetadataEntity.FIELD_NOTIFICATION_STATUS).append(" IN (");
        for (int i = 0; i < status.size(); i++) {
            expressionBuilder.append(":ns").append(i);
            if (i < status.size() - 1) {
                expressionBuilder.append(", ");
            }
            filterExpressionBuilder.putExpressionValue(":ns" + i, AttributeValue.builder().s(status.get(i).toString()).build());
        }
        expressionBuilder.append(")) ");
    }

    private void addSenderFilterExpression(String senderId,
                                           Expression.Builder filterExpressionBuilder,
                                           StringBuilder expressionBuilder) {
        if (!StringUtils.hasText(senderId)) {
            log.debug("senderId is empty - skip add senderId filter expression");
            return;
        }
        addEqStringFilterExpression(senderId, NotificationDelegationMetadataEntity.FIELD_SENDER_ID, ":senderId", filterExpressionBuilder, expressionBuilder);
    }

    private void addReceiverFilterExpression(String receiverId,
                                             Expression.Builder filterExpressionBuilder,
                                             StringBuilder expressionBuilder) {
        if (!StringUtils.hasText(receiverId)) {
            log.debug("receiverId is empty - skip add receiverId filter expression");
            return;
        }
        addEqStringFilterExpression(receiverId, NotificationDelegationMetadataEntity.FIELD_RECIPIENT_ID, ":recipientId", filterExpressionBuilder, expressionBuilder);
    }

    private void addEqStringFilterExpression(String value, String fieldName, String filterName, Expression.Builder filterExpressionBuilder, StringBuilder expressionBuilder) {
        addEventuallyAnd(expressionBuilder);
        expressionBuilder.append(fieldName).append(" = ").append(filterName).append(")");
        filterExpressionBuilder.putExpressionValue(filterName, AttributeValue.builder().s(value).build());
    }

    private void addEventuallyAnd(StringBuilder expressionBuilder) {
        expressionBuilder.append(expressionBuilder.length() > 0 ? " AND (" : " (");
    }

    private String retrieveAttributeName(IndexNameAndPartitions.SearchIndexEnum indexName) {
        return switch (indexName) {
            case INDEX_BY_DELEGATE -> NotificationDelegationMetadataEntity.FIELD_DELEGATE_ID_CREATION_MONTH;
            case INDEX_BY_DELEGATE_GROUP -> NotificationDelegationMetadataEntity.FIELD_DELEGATE_ID_GROUP_ID_CREATION_MONTH;
            default -> {
                String msg = String.format("Unable to retrieve attributeName by indexName=%s", indexName);
                log.error(msg);
                throw new PnInternalException(msg, ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
            }
        };
    }

    public void batchDeleteNotificationDelegated(List<NotificationDelegationMetadataEntity> deleteBatchItems) {

        WriteBatch.Builder<NotificationDelegationMetadataEntity> requestBuilder = WriteBatch
                .builder(NotificationDelegationMetadataEntity.class)
                .mappedTableResource(table);

        deleteBatchItems.forEach(item -> {
                    Key key = Key.builder().partitionValue(item.getIunRecipientIdDelegateIdGroupId())
                            .sortValue(item.getSentAt().toString()).build();
                    requestBuilder.addDeleteItem(key);
                }
        );

        dynamoDbEnhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .addWriteBatch(requestBuilder.build())
                .build());
    }
}

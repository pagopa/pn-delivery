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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME;

@Slf4j
@Component
public class NotificationDelegationMetadataEntityDaoDynamo
        extends AbstractDynamoKeyValueStore<NotificationDelegationMetadataEntity>
        implements NotificationDelegationMetadataEntityDao {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;


    private static final int DYNAMODB_MAX_BATCH_WRITE_ITEMS = 25;

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

        DynamoDbIndex<NotificationDelegationMetadataEntity> index = table.index(NotificationDelegationMetadataEntity.INDEX_MANDATE_ID);

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional(equalToConditional)
                .limit(size);

        if (lastEvaluatedKey != null && lastEvaluatedKey.getInternalLastEvaluatedKey() != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty()) {
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

    @Override
    public List<NotificationDelegationMetadataEntity> batchDeleteItems(List<NotificationDelegationMetadataEntity> items) {
        log.debug("batch delete items of {} elements", items.size());
        List<NotificationDelegationMetadataEntity> unprocessed = new ArrayList<>();
        if (items.isEmpty()) {
            log.debug("items is empty in batch delete items");
            return unprocessed;
        }
        for (int start = 0; start < items.size(); start = start + DYNAMODB_MAX_BATCH_WRITE_ITEMS) {
            int end = Math.min(start + DYNAMODB_MAX_BATCH_WRITE_ITEMS, items.size());
            log.trace("chunk start={} end={}", start, end);
            List<NotificationDelegationMetadataEntity> chunk = items.subList(start, end);
            List<NotificationDelegationMetadataEntity> chunkUnprocessed = execBatchDeleteItems(chunk);
            if (!chunkUnprocessed.isEmpty()) {
                log.debug("chunk {} to {} unprocessed: {}", start, end, chunkUnprocessed.size());
                unprocessed.addAll(chunkUnprocessed);
            }
        }
        if (!unprocessed.isEmpty()) {
            log.warn("batchDeleteItems has {} unprocessed items", unprocessed.size());
        }
        return unprocessed;
    }

    private List<NotificationDelegationMetadataEntity> execBatchDeleteItems(List<NotificationDelegationMetadataEntity> chunk) {
        WriteBatch.Builder<NotificationDelegationMetadataEntity> requestBuilder = WriteBatch
                .builder(NotificationDelegationMetadataEntity.class)
                .mappedTableResource(table);
        chunk.forEach(item -> {
                    Key key = Key.builder().partitionValue(item.getIunRecipientIdDelegateIdGroupId())
                            .sortValue(item.getSentAt().toString()).build();
                    requestBuilder.addDeleteItem(key);
                }
        );
        BatchWriteResult batchWriteResult = dynamoDbEnhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .addWriteBatch(requestBuilder.build())
                .build());
        return batchWriteResult.unprocessedPutItemsForTable(table);
    }

    @Override
    public List<NotificationDelegationMetadataEntity> batchPutItems(List<NotificationDelegationMetadataEntity> items) {
        log.debug("batch put items of {} elements", items.size());
        List<NotificationDelegationMetadataEntity> unprocessed = new ArrayList<>();
        if (items.isEmpty()) {
            log.debug("items is empty in batch put items");
            return unprocessed;
        }
        for (int start = 0; start < items.size(); start = start + DYNAMODB_MAX_BATCH_WRITE_ITEMS) {
            int end = Math.min(start + DYNAMODB_MAX_BATCH_WRITE_ITEMS, items.size());
            log.trace("chunk start={} end={}", start, end);

            List<NotificationDelegationMetadataEntity> chunk = items.subList(start, end);
            List<NotificationDelegationMetadataEntity> chunkUnprocessed = execBatchPutItems(chunk);

            if (!chunkUnprocessed.isEmpty()) {
                log.debug("chunk {} to {} unprocessed: {}", start, end, chunkUnprocessed.size());
                unprocessed.addAll(chunkUnprocessed);
            }
        }
        if (!unprocessed.isEmpty()) {
            log.warn("batchPutItems has {} unprocessed items", unprocessed.size());
        }
        return unprocessed;
    }

    private List<NotificationDelegationMetadataEntity> execBatchPutItems(List<NotificationDelegationMetadataEntity> chunk) {
        WriteBatch.Builder<NotificationDelegationMetadataEntity> builder =
                WriteBatch.builder(NotificationDelegationMetadataEntity.class)
                        .mappedTableResource(table);
        chunk.forEach(entity -> builder.addPutItem(req -> req.item(entity)));

        BatchWriteResult writeResult = dynamoDbEnhancedClient.batchWriteItem(BatchWriteItemEnhancedRequest.builder()
                .addWriteBatch(builder.build())
                .build());
        return writeResult.unprocessedPutItemsForTable(table);
    }

    @Override
    public Optional<NotificationDelegationMetadataEntity> deleteWithConditions(NotificationDelegationMetadataEntity entity) {
        Expression expression = Expression.builder()
                .expression(NotificationDelegationMetadataEntity.FIELD_MANDATE_ID + " = :mId")
                .putExpressionValue(":mId", AttributeValue.builder().s(entity.getMandateId()).build())
                .build();
        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                .key(k -> k
                        .partitionValue(entity.getIunRecipientIdDelegateIdGroupId())
                        .sortValue(entity.getSentAt().toString())
                        .build())
                .conditionExpression(expression)
                .build();
        try {
            return Optional.of(table.deleteItem(request));
        } catch (ConditionalCheckFailedException e) {
            log.warn("can not delete {} - conditional check failed", entity.getIunRecipientIdDelegateIdGroupId(), e);
            return Optional.empty();
        }
    }
}

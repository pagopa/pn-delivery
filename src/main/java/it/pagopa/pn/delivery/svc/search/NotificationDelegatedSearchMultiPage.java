package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA;

@Slf4j
public class NotificationDelegatedSearchMultiPage extends NotificationSearch {

    public static final int FILTER_EXPRESSION_APPLIED_MULTIPLIER = 4;
    public static final int MAX_DYNAMO_SIZE = 2000;

    private final NotificationDao notificationDao;
    private final PnLastEvaluatedKey lastEvaluatedKey;
    private final InputSearchNotificationDelegatedDto searchDto;
    private final PnDeliveryConfigs cfg;
    private final IndexNameAndPartitions indexNameAndPartitions;
    private final NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;

    public NotificationDelegatedSearchMultiPage(NotificationDao notificationDao,
                                                EntityToDtoNotificationMetadataMapper entityToDto,
                                                InputSearchNotificationDelegatedDto searchDto,
                                                PnLastEvaluatedKey lastEvaluatedKey,
                                                PnDeliveryConfigs cfg,
                                                PnDataVaultClientImpl dataVaultClient,
                                                NotificationDelegatedSearchUtils notificationDelegatedSearchUtils,
                                                IndexNameAndPartitions indexNameAndPartitions) {
        super(dataVaultClient, entityToDto);
        this.notificationDao = notificationDao;
        this.searchDto = searchDto;
        this.lastEvaluatedKey = lastEvaluatedKey;
        this.cfg = cfg;
        this.notificationDelegatedSearchUtils = notificationDelegatedSearchUtils;
        this.indexNameAndPartitions = indexNameAndPartitions;
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata() {
        log.info("notification delegated paged search indexName={}", indexNameAndPartitions.getIndexName());

        Integer maxPageNumber = searchDto.getMaxPageNumber() != null ? searchDto.getMaxPageNumber() : cfg.getMaxPageSize();

        int requiredSize = searchDto.getSize() * maxPageNumber + 1;
        int dynamoDbPageSize = requiredSize;
        if (!CollectionUtils.isEmpty(searchDto.getStatuses())
                || StringUtils.hasText(searchDto.getSenderId())
                || StringUtils.hasText(searchDto.getReceiverId())) {
            dynamoDbPageSize = dynamoDbPageSize * FILTER_EXPRESSION_APPLIED_MULTIPLIER;
        }

        PnLastEvaluatedKey startEvaluatedKey = lastEvaluatedKey;

        List<NotificationDelegationMetadataEntity> cumulativeQueryResult = new ArrayList<>();

        int iterations = 0;
        ResultPaginationDto<NotificationDelegationMetadataEntity, PnLastEvaluatedKey> queryResult;
        while (cumulativeQueryResult.size() < requiredSize) {
            queryResult = search(requiredSize, dynamoDbPageSize, startEvaluatedKey);
            List<NotificationDelegationMetadataEntity> filtered = notificationDelegatedSearchUtils.checkMandates(queryResult.getResultsPage(), searchDto);
            log.info("check mandates completed, preCheckCount={} postCheckCount={}", queryResult.getResultsPage().size(), filtered.size());
            cumulativeQueryResult.addAll(filtered);

            iterations++;

            // preparo una nuova iterazione, ma se non ho più pagine mi fermo
            if (CollectionUtils.isEmpty(queryResult.getNextPagesKey())) {
                break;
            }
            startEvaluatedKey = queryResult.getNextPagesKey().get(0);
        }

        log.info("search request completed, totalIterationCount={}, totalRowRead={}", iterations, cumulativeQueryResult.size());
        return prepareGlobalResult(cumulativeQueryResult, requiredSize);
    }

    private ResultPaginationDto<NotificationDelegationMetadataEntity, PnLastEvaluatedKey> search(int requiredSize, int dynamoDbPageSize, PnLastEvaluatedKey startEvaluatedKey) {
        int logItemCount = 0;

        List<NotificationDelegationMetadataEntity> dataRead = new ArrayList<>();
        int startIndex = 0;
        PnLastEvaluatedKey searchLastEvaluateKey = null;

        if (startEvaluatedKey != null) {
            startIndex = indexNameAndPartitions.getPartitions().indexOf(startEvaluatedKey.getExternalLastEvaluatedKey());
            log.debug("startEvaluatedKey is not null, starting search from index={}", startIndex);
        }

        for (int pIdx = startIndex; pIdx < indexNameAndPartitions.getPartitions().size(); pIdx++) {
            String currentPartition = indexNameAndPartitions.getPartitions().get(pIdx);
            logItemCount += readDataFromPartition(1, currentPartition, dataRead, startEvaluatedKey, requiredSize, dynamoDbPageSize);
            startEvaluatedKey = null;
            if (dataRead.size() >= requiredSize) {
                log.debug("reached required size, ending search");
                searchLastEvaluateKey = computeLastEvaluatedKey(dataRead.get(dataRead.size() - 1));
                searchLastEvaluateKey.setExternalLastEvaluatedKey(currentPartition);
                break;
            }
        }

        log.info("search request completed, totalDbQueryCount={}, totalRowRead={}", logItemCount, dataRead.size());

        ResultPaginationDto<NotificationDelegationMetadataEntity, PnLastEvaluatedKey> result = new ResultPaginationDto<>();
        result.setResultsPage(dataRead);
        result.setNextPagesKey(searchLastEvaluateKey != null ? List.of(searchLastEvaluateKey) : null);
        return result;
    }

    private int readDataFromPartition(int currentRequest, String partition, List<NotificationDelegationMetadataEntity> cumulativeQueryResult,
                                      PnLastEvaluatedKey lastEvaluatedKey,
                                      int requiredSize, int dynamoDbPageSize) {
        log.debug("START compute partition read trunk partition={} indexName={} currentRequest={} dynamoDbPageSize={}",
                partition, indexNameAndPartitions.getIndexName(), currentRequest, dynamoDbPageSize);

        PageSearchTrunk<NotificationDelegationMetadataEntity> oneQueryResult =
                notificationDao.searchDelegatedForOneMonth(searchDto,
                        indexNameAndPartitions.getIndexName(),
                        partition,
                        dynamoDbPageSize,
                        lastEvaluatedKey);
        log.debug("END search for one month indexName={} partitionValue={} dynamoDbPageSize={}",
                indexNameAndPartitions.getIndexName(), partition, dynamoDbPageSize);

        if (!CollectionUtils.isEmpty(oneQueryResult.getResults())) {
            cumulativeQueryResult.addAll(oneQueryResult.getResults());
        }

        if (cumulativeQueryResult.size() >= requiredSize) {
            log.debug("ending search, requiredSize reached - partition={} currentRequest={}", partition, currentRequest);
            return currentRequest;
        }

        if (oneQueryResult.getLastEvaluatedKey() != null) {
            log.debug("There are more data to read for partition={} currentRequest={} currentReadSize={}",
                    partition, currentRequest, cumulativeQueryResult.size());
            PnLastEvaluatedKey nextEvaluationKeyForSearch = new PnLastEvaluatedKey();
            nextEvaluationKeyForSearch.setExternalLastEvaluatedKey(partition);
            nextEvaluationKeyForSearch.setInternalLastEvaluatedKey(oneQueryResult.getLastEvaluatedKey());

            float multiplier = 2 - Math.min(oneQueryResult.getResults().size() / (float) requiredSize, 1);
            dynamoDbPageSize = Math.round(dynamoDbPageSize * multiplier);
            dynamoDbPageSize = Math.min(dynamoDbPageSize, MAX_DYNAMO_SIZE);

            return readDataFromPartition(currentRequest + 1, partition, cumulativeQueryResult, nextEvaluationKeyForSearch, requiredSize, dynamoDbPageSize);
        } else {
            log.debug("no more data to read for partition={} currentRequest={} currentReadSize={}", partition, currentRequest, cumulativeQueryResult.size());
            return currentRequest;
        }
    }

    private ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> prepareGlobalResult(List<NotificationDelegationMetadataEntity> queryResult,
                                                                                               int requiredSize) {
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult = new ResultPaginationDto<>();
        globalResult.setNextPagesKey(new ArrayList<>());

        globalResult.setResultsPage(queryResult.stream()
                .limit(searchDto.getSize())
                .map(metadata -> {
                    try {
                        return entityToDto.entity2Dto(metadata);
                    } catch (Exception e) {
                        String msg = String.format("Exception in mapping result for notification delegation metadata pk=%s", metadata.getIunRecipientIdDelegateIdGroupId());
                        throw new PnInternalException(msg, ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA, e);
                    }
                })
                .toList());

        globalResult.setMoreResult(queryResult.size() >= requiredSize);

        for (int i = 1; i <= cfg.getMaxPageSize(); i++) {
            int index = searchDto.getSize() * i;
            if (queryResult.size() <= index) {
                break;
            }
            NotificationDelegationMetadataEntity keyEntity = queryResult.get(index - 1);
            PnLastEvaluatedKey pageLastEvaluatedKey = computeLastEvaluatedKey(keyEntity);
            globalResult.getNextPagesKey().add(pageLastEvaluatedKey);
        }

        deanonimizeResults(globalResult);

        return globalResult;
    }

    private PnLastEvaluatedKey computeLastEvaluatedKey(NotificationDelegationMetadataEntity keyEntity) {
        PnLastEvaluatedKey pageLastEvaluatedKey = new PnLastEvaluatedKey();
        if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_DELEGATE_GROUP)) {
            pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyEntity.getDelegateIdGroupIdCreationMonth());
            pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                    NotificationDelegationMetadataEntity.FIELD_IUN_RECIPIENT_ID_DELEGATE_ID_GROUP_ID, AttributeValue.builder().s(keyEntity.getIunRecipientIdDelegateIdGroupId()).build(),
                    NotificationDelegationMetadataEntity.FIELD_DELEGATE_ID_GROUP_ID_CREATION_MONTH, AttributeValue.builder().s(keyEntity.getDelegateIdGroupIdCreationMonth()).build(),
                    NotificationDelegationMetadataEntity.FIELD_SENT_AT, AttributeValue.builder().s(keyEntity.getSentAt().toString()).build()
            ));
        } else if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_DELEGATE)) {
            pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyEntity.getDelegateIdCreationMonth());
            pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                    NotificationDelegationMetadataEntity.FIELD_IUN_RECIPIENT_ID_DELEGATE_ID_GROUP_ID, AttributeValue.builder().s(keyEntity.getIunRecipientIdDelegateIdGroupId()).build(),
                    NotificationDelegationMetadataEntity.FIELD_DELEGATE_ID_CREATION_MONTH, AttributeValue.builder().s(keyEntity.getDelegateIdCreationMonth()).build(),
                    NotificationDelegationMetadataEntity.FIELD_SENT_AT, AttributeValue.builder().s(keyEntity.getSentAt().toString()).build()
            ));
        }
        return pageLastEvaluatedKey;
    }

}

package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA;

@Slf4j
public class NotificationDelegatedSearchWithIun extends NotificationSearch {

    private final NotificationDao notificationDao;
    private PnLastEvaluatedKey lastEvaluatedKey;
    private final InputSearchNotificationDelegatedDto searchDto;
    private final PnDeliveryConfigs cfg;
    private final IndexNameAndPartitions indexNameAndPartitions;
    private final NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;

    public NotificationDelegatedSearchWithIun(NotificationDao notificationDao,
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
        List<NotificationDelegationMetadataEntity> toFiltered = new ArrayList<>();
        String startRecipientId;
        List<NotificationDelegationMetadataEntity> cumulativeQueryResult = new ArrayList<>();
        List<PnLastEvaluatedKey> lastEvaluatedKeys = new ArrayList<>();
        Integer maxPageNumber = searchDto.getMaxPageNumber() != null ? searchDto.getMaxPageNumber() : cfg.getMaxPageSize();
        int requiredSize = searchDto.getSize() * maxPageNumber + 1;
        Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(searchDto.getIun());
        if (optNotification.isPresent()) {
            List<String> recipientIds = optNotification.get().getRecipientIds();
            List<String> evaluatedRecipientIds;
            if (lastEvaluatedKey != null) {
                startRecipientId = lastEvaluatedKey.getExternalLastEvaluatedKey();
                evaluatedRecipientIds = recipientIds.subList(recipientIds.indexOf(startRecipientId), recipientIds.size());
            } else {
                lastEvaluatedKey = new PnLastEvaluatedKey();
                evaluatedRecipientIds = recipientIds;
                lastEvaluatedKey.setInternalLastEvaluatedKey(new HashMap<>());
                lastEvaluatedKey.setExternalLastEvaluatedKey(evaluatedRecipientIds.get(0));
            }

            do{
                evaluatedRecipientIds = evaluatedRecipientIds.subList(evaluatedRecipientIds.indexOf(lastEvaluatedKey.getExternalLastEvaluatedKey()), evaluatedRecipientIds.size());
                evaluatedRecipientIds.stream().limit(requiredSize).toList()
                        .forEach(recipientId -> {
                            searchDto.setReceiverId(recipientId);
                            toFiltered.addAll(searchByPk(searchDto));
                            PnLastEvaluatedKey evaluatedKey = new PnLastEvaluatedKey();
                            evaluatedKey.setInternalLastEvaluatedKey(new HashMap<>());
                            evaluatedKey.setExternalLastEvaluatedKey(recipientId);
                            lastEvaluatedKeys.add(evaluatedKey);
                        });
                List<NotificationDelegationMetadataEntity> filtered = new ArrayList<>(notificationDelegatedSearchUtils.checkMandates(toFiltered, searchDto));

                log.info("check mandates completed, preCheckCount=1 postCheckCount={}", filtered.size());
                cumulativeQueryResult.addAll(filtered);
            }while (cumulativeQueryResult.size() < requiredSize && evaluatedRecipientIds.size() > requiredSize);
        } else {
            log.info("Notification not found for iun=" + searchDto.getIun());
        }
        return prepareGlobalResult(cumulativeQueryResult, requiredSize, lastEvaluatedKeys);
    }

    private List<NotificationDelegationMetadataEntity> searchByPk(InputSearchNotificationDelegatedDto searchDto) {
        return notificationDao.searchByPk(searchDto).items();
    }

    private ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> prepareGlobalResult(List<NotificationDelegationMetadataEntity> queryResult,
                                                                                               int requiredSize,
                                                                                               List<PnLastEvaluatedKey> lastEvaluatedKeys){
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

        for (int i = 0; i <= cfg.getMaxPageSize(); i++) {
            int index = searchDto.getSize() * (i + 1);
            if (queryResult.size() <= index) {
                break;
            }
            globalResult.getNextPagesKey().add(lastEvaluatedKeys.get(i + searchDto.getSize()));
        }

        deanonimizeResults(globalResult);

        return globalResult;
    }
}

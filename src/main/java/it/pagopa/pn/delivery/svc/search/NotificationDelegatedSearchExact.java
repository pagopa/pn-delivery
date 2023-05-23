package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import java.util.*;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA;

@Slf4j
public class NotificationDelegatedSearchExact extends NotificationSearch {


    private final NotificationDao notificationDao;
    private final InputSearchNotificationDelegatedDto searchDto;
    private final PnDeliveryConfigs cfg;
    private final IndexNameAndPartitions indexNameAndPartitions;
    private final NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;

    public NotificationDelegatedSearchExact(NotificationDao notificationDao,
                                            EntityToDtoNotificationMetadataMapper entityToDto,
                                            InputSearchNotificationDelegatedDto searchDto,
                                            PnDeliveryConfigs cfg,
                                            PnDataVaultClientImpl dataVaultClient,
                                            NotificationDelegatedSearchUtils notificationDelegatedSearchUtils,
                                            IndexNameAndPartitions indexNameAndPartitions) {
        super(dataVaultClient, entityToDto);
        this.notificationDao = notificationDao;
        this.searchDto = searchDto;
        this.cfg = cfg;
        this.notificationDelegatedSearchUtils = notificationDelegatedSearchUtils;
        this.indexNameAndPartitions = indexNameAndPartitions;
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata() {
        log.info("notification delegated paged search indexName={}", indexNameAndPartitions.getIndexName());

        Integer maxPageNumber = searchDto.getMaxPageNumber() != null ? searchDto.getMaxPageNumber() : cfg.getMaxPageSize();
        int requiredSize = searchDto.getSize() * maxPageNumber + 1;
        NotificationDelegationMetadataEntity queryResult = searchByPk(searchDto);
        List<NotificationDelegationMetadataEntity> filtered = notificationDelegatedSearchUtils.checkMandates(List.of(queryResult), searchDto);
        log.info("check mandates completed, preCheckCount=1 postCheckCount={}", filtered.size());
        List<NotificationDelegationMetadataEntity> cumulativeQueryResult = new ArrayList<>(filtered);
        return prepareGlobalResult(cumulativeQueryResult, requiredSize);
    }

    private NotificationDelegationMetadataEntity searchByPk(InputSearchNotificationDelegatedDto searchDto) {
        Page<NotificationDelegationMetadataEntity> page = notificationDao.searchByPk(searchDto);
        if (page != null && !CollectionUtils.isEmpty(page.items())) {
            return page.items().get(0);
        }
        throw new PnNotificationNotFoundException("Notification not found");
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
        }
        deanonimizeResults(globalResult);

        return globalResult;
    }
}

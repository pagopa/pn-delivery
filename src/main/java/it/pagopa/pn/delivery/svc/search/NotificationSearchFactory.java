package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.springframework.stereotype.Component;

@Component
public class NotificationSearchFactory {

    private final NotificationDao notificationDao;
    private final EntityToDtoNotificationMetadataMapper entityToDto;
    private final PnDeliveryConfigs cfg;
    private final PnDataVaultClientImpl dataVaultClient;

    public NotificationSearchFactory(NotificationDao notificationDao, EntityToDtoNotificationMetadataMapper entityToDto, PnDeliveryConfigs cfg, PnDataVaultClientImpl dataVaultClient) {
        this.notificationDao = notificationDao;
        this.entityToDto = entityToDto;
        this.cfg = cfg;
        this.dataVaultClient = dataVaultClient;
    }

    public NotificationSearch getMultiPageSearch(InputSearchNotificationDto inputSearchNotificationDto,
                                                        PnLastEvaluatedKey lastEvaluatedKey){

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);

        if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_IUN))
            return new NotificationSearchExact(notificationDao, entityToDto, inputSearchNotificationDto, dataVaultClient);
        else
            return new NotificationSearchMultiPage(notificationDao, entityToDto, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
    }

    public NotificationSearch getMultiPageDelegatedSearch(InputSearchNotificationDelegatedDto searchDto,
                                                          PnLastEvaluatedKey lastEvaluatedKey) {
        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        return new NotificationDelegatedSearchMultiPage(notificationDao, entityToDto, searchDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
    }
}

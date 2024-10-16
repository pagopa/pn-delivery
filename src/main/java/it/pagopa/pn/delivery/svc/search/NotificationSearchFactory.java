package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationSearchFactory {

    private final NotificationDao notificationDao;
    private final EntityToDtoNotificationMetadataMapper entityToDto;
    private final PnDeliveryConfigs cfg;
    private final PnDataVaultClientImpl dataVaultClient;
    private final NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;

    public NotificationSearchFactory(NotificationDao notificationDao,
                                     EntityToDtoNotificationMetadataMapper entityToDto,
                                     PnDeliveryConfigs cfg,
                                     PnDataVaultClientImpl dataVaultClient,
                                     NotificationDelegatedSearchUtils notificationDelegatedSearchUtils){
        this.notificationDao = notificationDao;
        this.entityToDto = entityToDto;
        this.cfg = cfg;
        this.dataVaultClient = dataVaultClient;
        this.notificationDelegatedSearchUtils = notificationDelegatedSearchUtils;
    }

    public NotificationSearch getMultiPageSearch(InputSearchNotificationDto inputSearchNotificationDto,
                                                        PnLastEvaluatedKey lastEvaluatedKey){

        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectIndexAndPartitions(inputSearchNotificationDto);

        if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_IUN))
            return new NotificationSearchExact(notificationDao, entityToDto, inputSearchNotificationDto, dataVaultClient);
        else if ( StringUtils.hasText( inputSearchNotificationDto.getOpaqueFilterIdPF() ) &&
                StringUtils.hasText( inputSearchNotificationDto.getOpaqueFilterIdPG() ) ) {
            return new NotificationSearchMultiPageByPFAndPGOnly(notificationDao, entityToDto, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
        } else {
            return new NotificationSearchMultiPageByPFOrPG(notificationDao, entityToDto, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
        }
    }

    public NotificationSearch getMultiPageDelegatedSearch(InputSearchNotificationDelegatedDto searchDto,
                                                          PnLastEvaluatedKey lastEvaluatedKey) {
        IndexNameAndPartitions indexNameAndPartitions = IndexNameAndPartitions.selectDelegatedIndexAndPartitions(searchDto);
        if(StringUtils.hasText(searchDto.getIun()) && StringUtils.hasText(searchDto.getReceiverId())) {
            return new NotificationDelegatedSearchExact(notificationDao, entityToDto, searchDto, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
        }else if(StringUtils.hasText(searchDto.getIun())){
            return new NotificationDelegatedSearchWithIun(notificationDao, entityToDto, searchDto, lastEvaluatedKey, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
        }
        return new NotificationDelegatedSearchMultiPage(notificationDao, entityToDto, searchDto, lastEvaluatedKey, cfg, dataVaultClient, notificationDelegatedSearchUtils, indexNameAndPartitions);
    }
}

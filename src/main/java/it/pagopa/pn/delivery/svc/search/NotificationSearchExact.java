package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NotificationSearchExact extends NotificationSearch {

    private final NotificationDao notificationDao;
    private final InputSearchNotificationDto inputSearchNotificationDto;

    public NotificationSearchExact(NotificationDao notificationDao,
                                   EntityToDtoNotificationMetadataMapper entityToDto,
                                   InputSearchNotificationDto inputSearchNotificationDto,
                                   PnDataVaultClientImpl dataVaultClient) {
        super(dataVaultClient, entityToDto);
        this.notificationDao = notificationDao;
        this.inputSearchNotificationDto = inputSearchNotificationDto;
    }

    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata() {

        log.info( "notification exact search iun={}", inputSearchNotificationDto.getIunMatch() );

        PageSearchTrunk<NotificationMetadataEntity> res = notificationDao.searchByIUN(inputSearchNotificationDto);

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult = new ResultPaginationDto<>();
        globalResult.setMoreResult(false);
        globalResult.setResultsPage(new ArrayList<>());
        globalResult.setNextPagesKey(new ArrayList<>());

        if (!CollectionUtils.isEmpty(res.getResults())) {
            log.info("search by iun found");
            globalResult.setResultsPage(List.of(entityToDto.entity2Dto(res.getResults().get(0))));

            deanonimizeResults(globalResult);
        }
        else
        {
            log.info("search by iun not found");
        }
        return globalResult;
    }
}

package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NotificationSearchMultiPageByPFOrPG extends NotificationSearchMultiPage {



    public NotificationSearchMultiPageByPFOrPG(NotificationDao notificationDao,
                                               EntityToDtoNotificationMetadataMapper entityToDto,
                                               InputSearchNotificationDto inputSearchNotificationDto,
                                               PnLastEvaluatedKey lastEvaluatedKey,
                                               PnDeliveryConfigs cfg, PnDataVaultClientImpl dataVaultClient,
                                               IndexNameAndPartitions indexNameAndPartitions) {
        super(notificationDao, entityToDto, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
    }



    public List<NotificationMetadataEntity> getDataRead( Integer requiredSize, int dynamoDbPageSize ) {

        log.info( "notification paged search by PF or PG indexName={}", indexNameAndPartitions.getIndexName() );

        int logItemCount = 0;

        // mappa contenente le notifiche grezze, già filtrate per distinct, in modo che la size di questa mappa è congruente con il totale di elementi desiderati
        // la mappa viene popolata man mano che vengono eseguite le query verso dynamo
        List<NotificationMetadataEntity> dataRead = new ArrayList<>();
        int startIndex = 0;
        PnLastEvaluatedKey startEvaluatedKey = null;
        if (lastEvaluatedKey != null)
        {
            startEvaluatedKey = lastEvaluatedKey;
            startIndex = indexNameAndPartitions.getPartitions().indexOf(lastEvaluatedKey.getExternalLastEvaluatedKey());
            log.debug("lastEvaluatedKey is not null, starting search from index={}", startIndex);
        }


        // ciclo per ogni partizione, eventualmente scartando quelle non interessate in base alla lastEvaluatedKey
        for (int pIdx = startIndex;pIdx< indexNameAndPartitions.getPartitions().size();pIdx++ ) {

            String currentpartition = indexNameAndPartitions.getPartitions().get( pIdx );

            // legge tutti i dati dalla partizione
            logItemCount += readDataFromPartition(0, currentpartition, dataRead, startEvaluatedKey, requiredSize,  dynamoDbPageSize);

            // l'eventuale partizione iniziale ha senso SOLO per la prima partizione
            startEvaluatedKey = null;

            // se i dati letti sono più di quelli richiesti, posso concludere qui la ricerca
            if (dataRead.size() >= requiredSize)
            {
                log.debug("reached required size, ending search");
                break;
            }
        }

        log.info("search request completed, totalDbQueryCount={} totalRowRead={}", logItemCount, dataRead.size());

        return dataRead;
    }
}

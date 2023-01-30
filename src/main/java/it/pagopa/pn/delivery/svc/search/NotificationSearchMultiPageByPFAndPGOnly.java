package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class NotificationSearchMultiPageByPFAndPGOnly extends NotificationSearchMultiPage {

    public NotificationSearchMultiPageByPFAndPGOnly(NotificationDao notificationDao,
                                                    EntityToDtoNotificationMetadataMapper entityToDto,
                                                    InputSearchNotificationDto inputSearchNotificationDto,
                                                    PnLastEvaluatedKey lastEvaluatedKey,
                                                    PnDeliveryConfigs cfg, PnDataVaultClientImpl dataVaultClient,
                                                    IndexNameAndPartitions indexNameAndPartitions) {
        super(notificationDao, entityToDto, inputSearchNotificationDto, lastEvaluatedKey, cfg, dataVaultClient, indexNameAndPartitions);
    }


    /**
     * algoritmo: ricerca tutte le notifiche nella partizione senderId##recipientPF (più vecchie di eventuale lastEventKey passata)
     * ricerca tutte le notifiche nella partizione senderId##recipientPG (più vecchie di eventuale lastEventKey passata)
     * mergio risultati ordinandoli dal punto di vista temporale (sentAt)
     * in caso partendo dall'ultimo elemento ritornato dall'iterazione precedente (lastEvaluatedKey)
     * filtrando la size massima richiesta (requiredSize)
     *
     *
     * @param requiredSize dimensione elementi pagina FE
     * @param dynamoDbPageSize dimensione elementi pagina dynamoDB
     * @return lista delle entità di ricerca
     */
    public List<NotificationMetadataEntity> getDataRead( int requiredSize, int dynamoDbPageSize ) {

        log.info( "notification paged search by PF and PG indexName={}", indexNameAndPartitions.getIndexName() );

        int logItemCountPF = 0;
        int logItemCountPG = 0;

        List<NotificationMetadataEntity> dataReadPF = new ArrayList<>();
        List<NotificationMetadataEntity> dataReadPG = new ArrayList<>();

        Instant lastEvalutatedSentAt;
        PnLastEvaluatedKey lastEvaluatedKeyPF = null;
        PnLastEvaluatedKey lastEvaluatedKeyPG = null;
        if (lastEvaluatedKey != null)
        {
            // nel caso in cui sia arrivata una chiave, so per certo che posso iniziare a leggere i dati per quella partizione dalla chiave passata.
            // non so nulla dell'altra partizione, che pertanto vien letta tutta.
            if (indexNameAndPartitions.getPartitions().get( 0 ).equals(lastEvaluatedKey.getExternalLastEvaluatedKey()))
                lastEvaluatedKeyPF = lastEvaluatedKey;
            else
                lastEvaluatedKeyPG = lastEvaluatedKey;

            // nel campo sentAt, trovo il timestamp dell'ultimo record letto, mi interessano quelli "più vecchi" (l'ordinamento è decrescente)
            lastEvalutatedSentAt = Instant.parse(lastEvaluatedKey.getInternalLastEvaluatedKey().get(NotificationMetadataEntity.FIELD_SENT_AT).s());
        }
        else
            lastEvalutatedSentAt = null;


        // leggo TUTTE le righe di PF ( nella partizione 0 PF )
        logItemCountPF += readDataFromPartition( 0, indexNameAndPartitions.getPartitions().get( 0 ), dataReadPF, lastEvaluatedKeyPF, null, dynamoDbPageSize );
        // leggo TUTTE le righe di PG ( nella partizione 1 PG )
        logItemCountPG += readDataFromPartition( 0, indexNameAndPartitions.getPartitions().get( 1 ), dataReadPG, lastEvaluatedKeyPG, null, dynamoDbPageSize );

        List<NotificationMetadataEntity> dataRead = Stream.concat(dataReadPF.stream(), dataReadPG.stream()).toList();


        // faccio il merge in modo che dal punto di vista temporale siano ordinate
        // ritornando solo quelle con data più recente rispetto all'eventuale paginazione richiesta
        List<NotificationMetadataEntity> sortedNotificationMetadataEntities = dataRead.stream().sorted(
                Comparator.comparing( NotificationMetadataEntity::getSentAt ).reversed()
                )
                .filter(notif -> lastEvalutatedSentAt == null || notif.getSentAt().isAfter(lastEvalutatedSentAt))
                .limit(requiredSize)
                .toList();

        log.info("search request completed, totalDbQueryCount={} totalDbQueryCountPF={} totalDbQueryCountPG={} totalRowRead={} totalRowReadPF={} totalRowReadPG={} filteredRows={}",
                logItemCountPF + logItemCountPG, logItemCountPF, logItemCountPG, dataRead.size(), dataReadPF.size(), dataReadPG.size(), sortedNotificationMetadataEntities.size());
        return sortedNotificationMetadataEntities;

    }
}

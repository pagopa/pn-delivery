package it.pagopa.pn.delivery.svc.search;




import it.pagopa.pn.commons.exceptions.PnInternalException;
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
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA;

@Slf4j
public class NotificationSearchMultiPage extends NotificationSearch {

    public static final int FILTER_EXPRESSION_APPLIED_MULTIPLIER = 4;
    public static final int MAX_DYNAMO_SIZE = 2000;

    private final NotificationDao notificationDao;
    private final PnLastEvaluatedKey lastEvaluatedKey;
    private final InputSearchNotificationDto inputSearchNotificationDto;
    private final PnDeliveryConfigs cfg;
    private final IndexNameAndPartitions indexNameAndPartitions;

    public NotificationSearchMultiPage(NotificationDao notificationDao,
                                       EntityToDtoNotificationMetadataMapper entityToDto,
                                       InputSearchNotificationDto inputSearchNotificationDto,
                                       PnLastEvaluatedKey lastEvaluatedKey,
                                       PnDeliveryConfigs cfg, PnDataVaultClientImpl dataVaultClient,
                                       IndexNameAndPartitions indexNameAndPartitions) {
        super(dataVaultClient, entityToDto);
        this.notificationDao = notificationDao;
        this.inputSearchNotificationDto = inputSearchNotificationDto;
        this.lastEvaluatedKey = lastEvaluatedKey;
        this.cfg = cfg;
        this.indexNameAndPartitions = indexNameAndPartitions;
    }

    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata() {

        log.info( "notification paged search indexName={}", indexNameAndPartitions.getIndexName() );

        // - le API private impostano il numero di pagine richiesto. Le API esposte il valore è definito in configurazione
        Integer maxPageNumber = inputSearchNotificationDto.getMaxPageNumber() != null? inputSearchNotificationDto.getMaxPageNumber() : cfg.getMaxPageSize();

        // numero di elementi totali da cercare, di fatto la size della pagina * il numero di pagine + 1, così so per certo se ci sono altri elementi dopo o no.
        int requiredSize = inputSearchNotificationDto.getSize() * maxPageNumber + 1;
        // numero di elementi da chiedere a dynamoDb
        int dynamoDbPageSize = requiredSize;
        // se ho dei filtri ulteriori, suppongo che i dati vengano ulteriormente filtrati, quindi aumento il numero di elementi da leggere
        if (!CollectionUtils.isEmpty(inputSearchNotificationDto.getStatuses())
            || !CollectionUtils.isEmpty(inputSearchNotificationDto.getGroups())
            || (inputSearchNotificationDto.isBySender() && StringUtils.hasText(inputSearchNotificationDto.getFilterId())))
            dynamoDbPageSize = dynamoDbPageSize * FILTER_EXPRESSION_APPLIED_MULTIPLIER;

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

        return prepareGlobalResult(dataRead, requiredSize);
    }

    /**
     * Preparo i risultati da tornare al richiedente
     * @param cumulativeQueryResult risultati delle query
     * @param requiredSize dimensione totale richiesta
     * @return risultati ricerca
     */
    private ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> prepareGlobalResult(List<NotificationMetadataEntity> cumulativeQueryResult, int requiredSize) {

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult = new ResultPaginationDto<>();
        globalResult.setNextPagesKey(new ArrayList<>());

        // tronco i risultati alla dimensione della pagina
        globalResult.setResultsPage(cumulativeQueryResult.stream()
                .limit(inputSearchNotificationDto.getSize())
                .map(notificationMetadata ->{
                    try {
                        return entityToDto.entity2Dto(notificationMetadata);
                    } catch (Exception exc) {
                        String excMessage = String.format("Exception in mapping result for notificationMetadata iun###recipient_id=%s", notificationMetadata.getIunRecipientId());
                        throw new PnInternalException(excMessage, ERROR_CODE_DELIVERY_UNSUPPORTED_NOTIFICATION_METADATA, exc);
                    }
                })
                .toList());

        // dato che requiredSize era maggiore di 1, devo tornare che ci sono ancora elementi se la size è >= di required
        globalResult.setMoreResult(cumulativeQueryResult.size() >= requiredSize);

        // calcolo le varie pagine, a partire dai risultati
        for(int i = 1;i<=cfg.getMaxPageSize();i++)
        {
            int index = inputSearchNotificationDto.getSize()*i;
            if (cumulativeQueryResult.size() > index)
            {
                PnLastEvaluatedKey pageLastEvaluatedKey = new PnLastEvaluatedKey();
                NotificationMetadataEntity keyelement = cumulativeQueryResult.get(index-1);
                if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_SENDER))
                {
                    pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyelement.getSenderIdCreationMonth());
                    pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                            NotificationMetadataEntity.FIELD_SENDER_ID_CREATION_MONTH, AttributeValue.builder().s(keyelement.getSenderIdCreationMonth()).build(),
                            NotificationMetadataEntity.FIELD_SENT_AT, AttributeValue.builder().s(keyelement.getSentAt().toString()).build(),
                            NotificationMetadataEntity.FIELD_IUN_RECIPIENT_ID, AttributeValue.builder().s(keyelement.getIunRecipientId()).build()));
                }
                else if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_BY_RECEIVER))
                {
                    pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyelement.getRecipientIdCreationMonth());
                    pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                            NotificationMetadataEntity.FIELD_RECIPIENT_ID_CREATION_MONTH, AttributeValue.builder().s(keyelement.getRecipientIdCreationMonth()).build(),
                            NotificationMetadataEntity.FIELD_SENT_AT, AttributeValue.builder().s(keyelement.getSentAt().toString()).build(),
                            NotificationMetadataEntity.FIELD_IUN_RECIPIENT_ID, AttributeValue.builder().s(keyelement.getIunRecipientId()).build()));
                }  else if (indexNameAndPartitions.getIndexName().equals(IndexNameAndPartitions.SearchIndexEnum.INDEX_WITH_BOTH_IDS))
                {
                    pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyelement.getSenderIdRecipientId());
                    pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                            NotificationMetadataEntity.FIELD_SENDER_ID_RECIPIENT_ID, AttributeValue.builder().s(keyelement.getSenderIdRecipientId()).build(),
                            NotificationMetadataEntity.FIELD_SENT_AT, AttributeValue.builder().s(keyelement.getSentAt().toString()).build(),
                            NotificationMetadataEntity.FIELD_IUN_RECIPIENT_ID, AttributeValue.builder().s(keyelement.getIunRecipientId()).build()));
                }

                globalResult.getNextPagesKey().add(pageLastEvaluatedKey);
            }
            else
                break;
        }

        // faccio richiesta a data-vault per restituire i CF non opachi al FE
        deanonimizeResults(globalResult);

        return globalResult;
    }

    /**
     * Recupero tutti i dati da una partizione (ricorsivamente)
     * @param partition partizione di ricerca
     * @param cumulativeQueryResult risultati cumulativi da aggiornare
     * @param lastEvaluatedKey ultimo elemento letto nella partizione
     * @param requiredSize dimensione totale richiesta
     * @param dynamoDbPageSize dimensione della pagina da leggere in dynamo
     * @return true se ci sono altri dati da cercare, false altrimenti
     */
    private int readDataFromPartition(int currentRequest, String partition, List<NotificationMetadataEntity> cumulativeQueryResult,
                                       PnLastEvaluatedKey lastEvaluatedKey,
                                       int requiredSize, int dynamoDbPageSize) {
        log.debug( "START compute partition read trunk partition={} indexName={} currentRequest={} dynamoDbPageSize={}", partition,  indexNameAndPartitions.getIndexName(), currentRequest++, dynamoDbPageSize );

        PageSearchTrunk<NotificationMetadataEntity> oneQueryResult;
        oneQueryResult = notificationDao.searchForOneMonth(
                inputSearchNotificationDto,
                indexNameAndPartitions.getIndexName().getValue(),
                partition,
                dynamoDbPageSize,
                lastEvaluatedKey);
        log.debug( "END search for one month indexName={} partitionValue={} dynamoDbPageSize={}", indexNameAndPartitions.getIndexName(), partition, dynamoDbPageSize);

        // inserisco i risultati della query ad una singola partizione nei risultati cumulativi di ricerca
        // viene eseguito il "distinct" per IUN
        if (!CollectionUtils.isEmpty(oneQueryResult.getResults()))
            cumulativeQueryResult.addAll(oneQueryResult.getResults());

        if (cumulativeQueryResult.size() >= requiredSize)
        {
            log.debug("ending search, requiredSize reached  partition={} currentRequest={}", partition, currentRequest);
            return currentRequest;
        }

        if (oneQueryResult.getLastEvaluatedKey() != null)
        {
            log.debug("thare are more data to read for partition={} currentRequest={} currentReadSize={}", partition, currentRequest, cumulativeQueryResult.size());
            PnLastEvaluatedKey nextEvaluationKeyForSearch = new PnLastEvaluatedKey();
            nextEvaluationKeyForSearch.setExternalLastEvaluatedKey(partition);
            nextEvaluationKeyForSearch.setInternalLastEvaluatedKey(oneQueryResult.getLastEvaluatedKey());

            // mi adatto in base a quanti dati ho letto, se non leggo niente, di fatto raddoppio la size fino al MAX configurato
            // se ho letto "abbastanza", di fatto leggo al più altrettanto

            float multiplier = 2 - Math.min(((float)oneQueryResult.getResults().size() / (float)requiredSize), 1);
            dynamoDbPageSize = Math.round(dynamoDbPageSize * multiplier);
            dynamoDbPageSize = Math.min(dynamoDbPageSize, MAX_DYNAMO_SIZE);

            return readDataFromPartition(currentRequest, partition, cumulativeQueryResult, nextEvaluationKeyForSearch, requiredSize, dynamoDbPageSize);
        }
        else
        {
            log.debug("no more data to read for partition={} currentRequest={} currentReadSize={}", partition, currentRequest, cumulativeQueryResult.size());
            return currentRequest;
        }
    }
}

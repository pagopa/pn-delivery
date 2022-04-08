package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiPageSearch {

    private final NotificationDao notificationDao;
    private String indexName;
    private final PnLastEvaluatedKey lastEvaluatedKey;
    private final InputSearchNotificationDto inputSearchNotificationDto;
    private final PnDeliveryConfigs cfg;

    public MultiPageSearch(NotificationDao notificationDao,
                           InputSearchNotificationDto inputSearchNotificationDto,
                           PnLastEvaluatedKey lastEvaluatedKey,
                           PnDeliveryConfigs cfg) {
        this.notificationDao = notificationDao;
        this.inputSearchNotificationDto = inputSearchNotificationDto;
        this.lastEvaluatedKey = lastEvaluatedKey;
        this.cfg = cfg;
    }

    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata() {

        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult = new ResultPaginationDto<>();

        // recupero dell'indice dove andrò ad eseguire la query di ricerca
        retrieveIndexName(inputSearchNotificationDto);

        // nel caso di ricerche multi mese elenca le partizioni mensili di ricerca dalla partizione più recente a quella più lontana
        // per ricerche che non dipendono dal mese restituisce una singola partizione
        List<String> partitions = listMonthPartitions( inputSearchNotificationDto, lastEvaluatedKey );

        int pIdx = 0;
        int numPages = 0;
        int missingLinesOnPage = inputSearchNotificationDto.getSize();

        while ( numPages < cfg.getMaxPageSize() && pIdx < partitions.size() ) {

            String partition = partitions.get( pIdx );

            // recupero della LastEvaluatedKey se presente quella della query precedente
            // altrimenti quella proveniente dal FE, null nel caso di prima ricerca
            PnLastEvaluatedKey oneMonthKey;
            if( globalResult.getNextPagesKey() != null && !globalResult.getNextPagesKey().isEmpty() ) {
                int lastIndex = globalResult.getNextPagesKey().size();
                oneMonthKey = globalResult.getNextPagesKey().get( lastIndex - 1 );
            }
            else {
                oneMonthKey = lastEvaluatedKey;
            }

            // calcolo del valore della partizione dove verrà effettuata la query
            // nel caso di partizioni mensili dipende dal mese di interesse (partition)
            // se valorizzata dipende dalla LastEvaluatedKey fornita dal FE per query a pagine successive alla prima
            String partitionValue = computePartitionValue( inputSearchNotificationDto, partition, lastEvaluatedKey );

            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> oneQueryResult;
            oneQueryResult = notificationDao.searchForOneMonth(
                    inputSearchNotificationDto,
                    indexName,
                    partitionValue,
                    missingLinesOnPage,
                    oneMonthKey);

            // inserisco i risultati della query ad una singola partizione nei risultati globali di ricerca
            if( numPages == 0 ) {
                List<NotificationSearchRow> oldResult = globalResult.getResult();
                if (oldResult != null ) {
                    oldResult.addAll( oneQueryResult.getResult() );
                } else {
                    globalResult.setResult( oneQueryResult.getResult() );
                }

            }

            // aggiorno il numero di elementi ancora restituibili al FE in base ai risultati ottenuti
            // dalla singola query sulla partizione
            int retrievedRowsNum = oneQueryResult.getResult().size();
            missingLinesOnPage -= retrievedRowsNum;

            // se non posso restituire altri elementi al FE allora pagina dei risultati è completa,
            // quindi proseguo per riempire altra pagina oppure mi sposto a partizione mensile precedente
            if( missingLinesOnPage == 0 ) {
                missingLinesOnPage = inputSearchNotificationDto.getSize();
            }
            else {
                pIdx += 1;
            }

            // aggiorno la lista delle LastEvaluatedKey da restituire al FE per farmi interrogare direttamente la pagina richiesta
            if( oneQueryResult.getNextPagesKey() != null ) {
                List<PnLastEvaluatedKey> oldLastEvaluatedKey = globalResult.getNextPagesKey();
                if ( oldLastEvaluatedKey != null ) {
                    oldLastEvaluatedKey.addAll(oneQueryResult.getNextPagesKey());
                    globalResult.setNextPagesKey(oldLastEvaluatedKey);
                }
                else {
                    globalResult.setNextPagesKey( oneQueryResult.getNextPagesKey() );
                }
                numPages = globalResult.getNextPagesKey().size();
            }
        }
        return globalResult;
    }

    private void retrieveIndexName(InputSearchNotificationDto inputSearchNotificationDto) {
        final String filterId = inputSearchNotificationDto.getFilterId();
        if(inputSearchNotificationDto.isBySender()) {
            indexName = NotificationMetadataEntity.INDEX_SENDER_ID;
        } else {
            indexName = NotificationMetadataEntity.INDEX_RECIPIENT_ID;
        }
        if (filterId != null) {
            indexName = NotificationMetadataEntity.INDEX_SENDER_ID_RECIPIENT_ID;
        }
    }

    private List<String> listMonthPartitions( InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey ) {
        if ( inputSearchNotificationDto.getFilterId() != null ){
            return Collections.singletonList( "noMonthPartition" );
        }
        Instant endDate = inputSearchNotificationDto.getEndDate();
        if (lastEvaluatedKey != null) {
            endDate = Instant.parse( lastEvaluatedKey.getInternalLastEvaluatedKey().get( "sentAt" ).s() );
        }
        return retrieveCreationMonth(inputSearchNotificationDto.getStartDate(), endDate);
    }

    private List<String> retrieveCreationMonth(Instant startDate, Instant endDate) {
        List<String> creationMonths = new ArrayList<>();
        ZonedDateTime currentMonth = ZonedDateTime.ofInstant( startDate, ZoneId.of( "UTC" ) )
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.firstDayOfMonth());
        while ( currentMonth.toInstant().isBefore( endDate )  || currentMonth.toInstant().equals( endDate )) {
            String[] splitCurrentMonth = currentMonth.toString().split( "-" );
            String currentMonthString = splitCurrentMonth[0] + splitCurrentMonth[1];
            creationMonths.add( currentMonthString );
            currentMonth = currentMonth.plus( 1, ChronoUnit.MONTHS );
        }
        Collections.reverse( creationMonths );
        return creationMonths;
    }

    private String computePartitionValue(InputSearchNotificationDto inputSearchNotificationDto, String oneMonth, PnLastEvaluatedKey lastEvaluatedKey) {
        String partitionValue;
        final String senderReceiverId = inputSearchNotificationDto.getSenderReceiverId();
        final String filterId = inputSearchNotificationDto.getFilterId();
        if (filterId != null) {
            partitionValue = senderReceiverId
                    + "##" + filterId;
        } else if (lastEvaluatedKey != null && oneMonth.equals( lastEvaluatedKey.getExternalLastEvaluatedKey()
                .substring( lastEvaluatedKey.getExternalLastEvaluatedKey().indexOf( "##" )+2 ) )) {
            partitionValue = lastEvaluatedKey.getExternalLastEvaluatedKey();
        } else {
            partitionValue = senderReceiverId + "##" + oneMonth;
        }
        return partitionValue;
    }
}

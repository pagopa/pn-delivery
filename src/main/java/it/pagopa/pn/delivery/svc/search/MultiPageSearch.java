package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationDao;

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

        retrieveIndexName(inputSearchNotificationDto);

        List<String> partitions = listMonthPartitions( inputSearchNotificationDto, lastEvaluatedKey );

        int pIdx = 0;
        int numPages = 0;
        int numeroDiRigheMancantiNellaPagina = inputSearchNotificationDto.getSize();

        while ( numPages < cfg.getMaxPageSize() && pIdx < partitions.size() ) {

            String partition = partitions.get( pIdx );

            Instant startDate = inputSearchNotificationDto.getStartDate();
            Instant endDate = inputSearchNotificationDto.getEndDate();

            PnLastEvaluatedKey oneMonthKey;
            if( globalResult.getNextPagesKey() != null && !globalResult.getNextPagesKey().isEmpty() ) {
                int lastIndex = globalResult.getNextPagesKey().size();
                oneMonthKey = globalResult.getNextPagesKey().get( lastIndex - 1 );
            }
            else {
                oneMonthKey = lastEvaluatedKey;
            }

            String partitionValue = computePartitionValue( inputSearchNotificationDto, partition, lastEvaluatedKey );

            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> oneQueryResult;
            oneQueryResult = notificationDao.searchForOneMonth(
                    inputSearchNotificationDto,
                    indexName,
                    startDate,
                    endDate,
                    partitionValue,
                    numeroDiRigheMancantiNellaPagina,
                    oneMonthKey);

            if( numPages == 0 ) {
                List<NotificationSearchRow> oldResult = globalResult.getResult();
                if (oldResult != null ) {
                    oldResult.addAll( oneQueryResult.getResult() );
                } else {
                    globalResult.setResult( oneQueryResult.getResult() );
                }

            }

            int retrievedRowsNum = oneQueryResult.getResult().size();
            numeroDiRigheMancantiNellaPagina -= retrievedRowsNum;

            if( numeroDiRigheMancantiNellaPagina == 0 ) {
                numeroDiRigheMancantiNellaPagina = inputSearchNotificationDto.getSize();
            }
            else {
                pIdx += 1;
            }

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
            indexName = "senderId";
            if (filterId != null) {
                indexName += "_recipientId";
            }
        } else {
            indexName = "recipientId";
            if (filterId != null) {
                indexName = "senderId_" + indexName;
            }
        }
    }

    private List<String> listMonthPartitions( InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey ) {
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

package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationMetadataEntityDaoDynamo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MultiPageSearch {

    private static final int MAX_PAGES = 4; // TODO da prendere dalla cfg
    private final NotificationMetadataEntityDaoDynamo notificationDao;

    public MultiPageSearch(NotificationMetadataEntityDaoDynamo notificationDao) {
        this.notificationDao = notificationDao;
    }

    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata(
            InputSearchNotificationDto inputSearchNotificationDto,
            PnLastEvaluatedKey lastEvaluatedKey
    ) {
        ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult = new ResultPaginationDto<>();
        String indexName = notificationDao.retrieveIndexName(inputSearchNotificationDto);

        List<String> partitions = listMonthPartitions( inputSearchNotificationDto, lastEvaluatedKey );

        int pIdx = 0;
        int numPages = 0;
        int numeroDiRigheMancantiNellaPagina = inputSearchNotificationDto.getSize();

        while ( numPages < MAX_PAGES && pIdx < partitions.size() ) {

            String partition = partitions.get( pIdx );

            String startDate = inputSearchNotificationDto.getStartDate().toString();
            String endDate = inputSearchNotificationDto.getEndDate().toString();

            PnLastEvaluatedKey oneMonthKey;
            if( globalResult.getNextPagesKey() != null && !globalResult.getNextPagesKey().isEmpty() ) {
                int lastIndex = globalResult.getNextPagesKey().size();
                oneMonthKey = globalResult.getNextPagesKey().get( lastIndex - 1 );
            }
            else {
                oneMonthKey = lastEvaluatedKey;
            }

            ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> oneQueryResult;
            oneQueryResult = notificationDao.searchForOneMonth(
                    inputSearchNotificationDto,
                    indexName,
                    startDate,
                    endDate,
                    partition,
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
                if ( oldLastEvaluatedKey != null )
                    oldLastEvaluatedKey.addAll( oneQueryResult.getNextPagesKey() );
                else {
                    globalResult.setNextPagesKey( oneQueryResult.getNextPagesKey() );
                }
                numPages = globalResult.getNextPagesKey().size();
            }

        }

        return globalResult;
    }

    private List<String> listMonthPartitions( InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey ) {
        Instant endDate = inputSearchNotificationDto.getEndDate();
        if (lastEvaluatedKey != null) {
            endDate = Instant.parse( lastEvaluatedKey.getInternalLastEvaluatedKey().get( "sentAt" ).s() );
        }
        return notificationDao.retrieveCreationMonth(inputSearchNotificationDto.getStartDate(), endDate);
    }
}

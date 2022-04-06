package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao<Key, NotificationMetadataEntity> {
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private EntityToDtoNotificationMetadataMapper entityToDto;

    protected NotificationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, EntityToDtoNotificationMetadataMapper entityToDto, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationMetadataEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.entityToDto = entityToDto;
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationMetadataDao().getTableName();
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchNotificationMetadata(InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey) {

        Integer maxRowNum = inputSearchNotificationDto.getSize();
        if ( maxRowNum == null || maxRowNum <= 0 ) {
            throw new PnInternalException( "Unable to paginate search result without requested size" );
        }

        Instant endDateInstant = inputSearchNotificationDto.getEndDate();
        if (lastEvaluatedKey != null) {
            endDateInstant = Instant.parse( lastEvaluatedKey.getInternalLastEvaluatedKey().get( "sentAt" ).s() );
        }
        final Instant startDateInstant = inputSearchNotificationDto.getStartDate();
        final String startDate = startDateInstant.toString();
        final String endDate = endDateInstant.toString();

        List<String> creationMonths = retrieveCreationMonth( startDateInstant, endDateInstant );

        String indexName = retrieveIndexName( inputSearchNotificationDto ) ;

        List<NotificationSearchRow> rows = new ArrayList<>();
        List<PnLastEvaluatedKey> nextPagesKey = new ArrayList<>();

        for ( String oneMonth : creationMonths) {
            ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> oneMonthResults = searchForOneMonth(
                    inputSearchNotificationDto,
                    indexName,
                    startDate,
                    endDate,
                    oneMonth,
                    maxRowNum,
                    lastEvaluatedKey);

            rows.addAll( oneMonthResults.getResult() );
            maxRowNum -= oneMonthResults.getResult().size();

            if( oneMonthResults.getNextPagesKey() != null ){
                nextPagesKey.addAll( oneMonthResults.getNextPagesKey() );
                lastEvaluatedKey = oneMonthResults.getNextPagesKey().get( 0 );
            } else {
                lastEvaluatedKey = null;
            }
            if (maxRowNum > rows.size() ) {
                lastEvaluatedKey = null;
            }
            if (maxRowNum <= 0) {
                maxRowNum = inputSearchNotificationDto.getSize();
            }
        }
        boolean moreResult = rows.size() >= inputSearchNotificationDto.getSize();
        return ResultPaginationDto.<NotificationSearchRow,PnLastEvaluatedKey>builder()
                .result( rows )
                .moreResult( moreResult )
                .nextPagesKey( moreResult? nextPagesKey : Collections.emptyList() )
                .build();
    }

    @NotNull
    public ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String startDate,
            String endDate,
            String oneMonth,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    ) {

        String partitionValue = computePartitionValue( inputSearchNotificationDto, oneMonth, lastEvaluatedKey );

        Key.Builder builder = Key.builder().partitionValue(partitionValue);
        Key key = builder.build();
        Key key1 = builder.sortValue(startDate).build();
        Key key2 = builder.sortValue(endDate).build();

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo( key );
        QueryConditional betweenConditional = QueryConditional
                .sortBetween( key1, key2 );

        DynamoDbIndex<NotificationMetadataEntity> index = table.index( indexName );

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional( queryConditional )
                .queryConditional( betweenConditional )
                .limit( size )
                .scanIndexForward( false );

        addFilterExpression(inputSearchNotificationDto, requestBuilder);

        if( lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty() ) {
            requestBuilder.exclusiveStartKey( lastEvaluatedKey.getInternalLastEvaluatedKey() );
        }

        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( requestBuilder.build() );

        Page<NotificationMetadataEntity> page = notificationMetadataPages.iterator().next();

        ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,PnLastEvaluatedKey> resultPaginationDtoBuilder = ResultPaginationDto.builder();
        resultPaginationDtoBuilder.result( fromNotificationMetadataToNotificationSearchRow( page.items() )).moreResult( false );

        if ( page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty()) {
            PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
            pnLastEvaluatedKey.setExternalLastEvaluatedKey( partitionValue  );
            pnLastEvaluatedKey.setInternalLastEvaluatedKey( page.lastEvaluatedKey() );
            List<PnLastEvaluatedKey> lastEvaluatedKeyList = new ArrayList<>();
            lastEvaluatedKeyList.add( pnLastEvaluatedKey );
            resultPaginationDtoBuilder.nextPagesKey( lastEvaluatedKeyList )
                    .moreResult( true );
        }
        return resultPaginationDtoBuilder.build();
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

    public String retrieveIndexName(InputSearchNotificationDto inputSearchNotificationDto) {
        String indexName;
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
        return indexName;
    }

    private void addFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                     QueryEnhancedRequest.Builder requestBuilder) {
        addStatusFilterExpression(inputSearchNotificationDto, requestBuilder);
        addGroupFilterExpression(inputSearchNotificationDto, requestBuilder);
    }

    private void addStatusFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                           QueryEnhancedRequest.Builder requestBuilder) {
        if ( inputSearchNotificationDto.getStatus() != null ) {
            Expression filterStatusExpression = Expression.builder()
                    .expression( "notificationStatus = :notificationStatusValue" )
                    .putExpressionValue(
                            ":notificationStatusValue",
                            AttributeValue.builder()
                                    .s( inputSearchNotificationDto.getStatus().toString() )
                                    .build()
                    ).build();
            requestBuilder.filterExpression( filterStatusExpression );
        }
    }

    private void addGroupFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                          QueryEnhancedRequest.Builder requestBuilder) {
        if ( inputSearchNotificationDto.getGroups() != null ) {
            List<String> queries = new ArrayList<>();
            List<String> groupList = inputSearchNotificationDto.getGroups();
            Map<String,AttributeValue> mav = new HashMap<>();
            for (int i = 0; i < groupList.size(); i++) {
                String placeHolder = ":val" + i;
                mav.put(placeHolder, AttributeValue.builder()
                                .s( groupList.get(i) )
                        .build());
                queries.add("notificationGroup = " + placeHolder);
            }

            String query = String.join(" or ", queries);
            requestBuilder.filterExpression( Expression.builder()
                    .expression( query )
                    .expressionValues( mav )
                    .build());
        }
    }

    private List<NotificationSearchRow> fromNotificationMetadataToNotificationSearchRow(List<NotificationMetadataEntity> metadataEntityList) {
        List<NotificationSearchRow> result = new ArrayList<>();
        metadataEntityList.forEach( entity -> result.add( entityToDto.entity2Dto( entity )) );
        return result;
    }

    public List<String> retrieveCreationMonth(Instant startDate, Instant endDate) {
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

    @Override
    public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) throws IdConflictException {
        PutItemEnhancedRequest<NotificationMetadataEntity> request = PutItemEnhancedRequest.
                builder(NotificationMetadataEntity.class)
                .item( notificationMetadataEntity )
                .build();
        table.putItem( request );
    }
}

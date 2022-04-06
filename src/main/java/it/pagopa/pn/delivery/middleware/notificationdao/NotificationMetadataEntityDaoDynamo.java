package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao {
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
    public ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey>    searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            Instant startDate,
            Instant endDate,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    ) {

        Key.Builder builder = Key.builder().partitionValue(partitionValue);
        Key key = builder.build();
        Key key1 = builder.sortValue(startDate.toString()).build();
        Key key2 = builder.sortValue(endDate.toString()).build();

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
            //TODO recuperare attributeHashKey da indexName
            if ( lastEvaluatedKey.getInternalLastEvaluatedKey().get( "senderId_creationMonth" ).s().equals( partitionValue ) ) {
                requestBuilder.exclusiveStartKey(lastEvaluatedKey.getInternalLastEvaluatedKey());
            }
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



    @Override
    public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) {
        PutItemEnhancedRequest<NotificationMetadataEntity> request = PutItemEnhancedRequest.
                builder(NotificationMetadataEntity.class)
                .item( notificationMetadataEntity )
                .build();
        table.putItem( request );
    }
}

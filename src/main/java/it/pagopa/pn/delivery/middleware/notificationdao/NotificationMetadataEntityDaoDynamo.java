package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao {
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private EntityToDtoNotificationMetadataMapper entityToDto;
    private PnDataVaultClientImpl dataVaultClient;

    private static final Instant PN_EPOCH = Instant.ofEpochSecond( 1651399200 ); // 2022-05-01T12:00:00.000 GMT+2:00

    protected NotificationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, EntityToDtoNotificationMetadataMapper entityToDto, PnDeliveryConfigs cfg, PnDataVaultClientImpl dataVaultClient) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationMetadataEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.entityToDto = entityToDto;
        this.dataVaultClient = dataVaultClient;
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationMetadataDao().getTableName();
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    ) {
        log.debug( "START search for one month" );
        Instant startDate = inputSearchNotificationDto.getStartDate();
        Instant endDate = inputSearchNotificationDto.getEndDate();

        if( PN_EPOCH.isAfter( startDate ) ) {
            startDate = PN_EPOCH;
        }

        log.debug( "Key building ..." );
        // costruzione delle Keys di ricerca in base alla partizione che si vuole interrogare ed al range di date di interesse
        Key.Builder builder = Key.builder().partitionValue(partitionValue);
        Key key1 = builder.sortValue(startDate.toString()).build();
        Key key2 = builder.sortValue(endDate.toString()).build();
        log.debug( " ... key building done " +
                "startKeyPartition={} startKeyRange={} endKeyPartition={} endKeyRange={}",
                key1.partitionKeyValue(), key1.sortKeyValue(),
                key2.partitionKeyValue(), key2.sortKeyValue()
            );

        log.debug( "Create query conditional" );
        QueryConditional betweenConditional = QueryConditional
                .sortBetween( key1, key2 );

        DynamoDbIndex<NotificationMetadataEntity> index = table.index( indexName );

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional( betweenConditional )
                .limit( size )
                .scanIndexForward( false );

        log.debug( "START add filter expression" );
        // aggiunta dei filtri alla query: status, groups, iun
        addFilterExpression(inputSearchNotificationDto.getStatuses(),
                inputSearchNotificationDto.getGroups(),
                inputSearchNotificationDto.getIunMatch(),
                requestBuilder);
        log.debug( "END add filter expression" );

        // se query su partizione precedente ha restituito una LEK
        // recupero nome dell'attributo in base all'indice di ricerca ed imposto
        // l'ultimo elemento valutato nella query precedente come exclusiveStartKey della query che segue
        if( lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty() ) {
            String attributeName = retrieveAttributeName( indexName );
            if ( lastEvaluatedKey.getInternalLastEvaluatedKey().get( attributeName ).s().equals( partitionValue ) ) {
                requestBuilder.exclusiveStartKey(lastEvaluatedKey.getInternalLastEvaluatedKey());
            }
        }

        log.debug( "START query execution" );
        // eseguo la query
        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( requestBuilder.build() );
        log.debug( "END query execution" );

        // recupero i risultati della query
        Page<NotificationMetadataEntity> page = notificationMetadataPages.iterator().next();

        // imposto i risultati della query mappandoli da NotificationMetadata a NotificationSearchRow
        ResultPaginationDto.ResultPaginationDtoBuilder<NotificationSearchRow,PnLastEvaluatedKey> resultPaginationDtoBuilder = ResultPaginationDto.builder();
        resultPaginationDtoBuilder.resultsPage( fromNotificationMetadataToNotificationSearchRow( page.items() )).moreResult( false );

        // imposto la LEK in base al risultato della query
        if ( page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty()) {
            PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
            pnLastEvaluatedKey.setExternalLastEvaluatedKey( partitionValue  );
            pnLastEvaluatedKey.setInternalLastEvaluatedKey( page.lastEvaluatedKey() );
            List<PnLastEvaluatedKey> lastEvaluatedKeyList = new ArrayList<>();
            lastEvaluatedKeyList.add( pnLastEvaluatedKey );
            resultPaginationDtoBuilder.nextPagesKey( lastEvaluatedKeyList )
                    .moreResult( true );
        }
        log.debug( "END mapper from metadata to searchRow" );
        return resultPaginationDtoBuilder.build();
    }

    private String retrieveAttributeName(String indexName) {
        String attributeName;
        switch ( indexName ) {
            case NotificationMetadataEntity.FIELD_SENDER_ID:
                attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_CREATION_MONTH; break;
            case NotificationMetadataEntity.FIELD_RECIPIENT_ID:
                attributeName = NotificationMetadataEntity.FIELD_RECIPIENT_ID_CREATION_MONTH; break;
            case NotificationMetadataEntity.INDEX_SENDER_ID_RECIPIENT_ID:
                attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_RECIPIENT_ID; break;
            default: {
                String msg = String.format( "Unable to retrieve attributeName by indexName=%s", indexName );
                log.error( msg );
                throw new PnInternalException( msg );
            }
        }
        return attributeName;
    }

    private void addFilterExpression(List<NotificationStatus> statuses,
                                        List<String> groups,
                                        String iunMatch,
                                        QueryEnhancedRequest.Builder requestBuilder
    ) {
        addStatusFilterExpression( statuses, requestBuilder);
        addGroupFilterExpression( groups, requestBuilder);
        addIunFilterExpression( iunMatch, requestBuilder );
    }

    private void addStatusFilterExpression(List<NotificationStatus> statuses,
                                           QueryEnhancedRequest.Builder requestBuilder) {
        if (!CollectionUtils.isEmpty(statuses)) {
            Expression.Builder filterStatusExpressionBuilder = Expression.builder();

            StringBuilder exp = new StringBuilder();
            for (int i = 0;i<statuses.size();i++) {
                NotificationStatus notificationStatus = statuses.get(i);
                exp.append("notificationStatus = :notificationStatusValue");
                exp.append(i + " ");
                if (i<statuses.size()-1)
                    exp.append(" OR ");

                filterStatusExpressionBuilder.putExpressionValue(":notificationStatusValue"+i,
                        AttributeValue.builder()
                                .s( notificationStatus.toString() )
                                .build());
            }

            filterStatusExpressionBuilder.expression(exp.toString());
            requestBuilder.filterExpression( filterStatusExpressionBuilder.build() );
        }
    }

    private void addIunFilterExpression(String iunMatch,
                                        QueryEnhancedRequest.Builder requestBuilder) {
        if ( iunMatch != null ) {
            log.debug( "Add iun filter expression" );
            Expression filterIunExpression = Expression.builder()
                    .expression( "begins_with(iun_recipientId, :iunValue)" )
                    .putExpressionValue(
                            ":iunValue",
                            AttributeValue.builder()
                                    .s( iunMatch )
                                    .build()
                    ).build();
            requestBuilder.filterExpression( filterIunExpression );
        }
    }

    private void addGroupFilterExpression(List<String> groupList,
                                          QueryEnhancedRequest.Builder requestBuilder) {
        if ( groupList != null ) {
            log.debug( "Add group filter expression" );
            List<String> queries = new ArrayList<>();
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
        log.debug( "START mapper from metadata to searchRow" );
        List<NotificationSearchRow> result = new ArrayList<>();
        Map<String, NotificationMetadataEntity> metadataEntityMap = new HashMap<String,NotificationMetadataEntity>();
        for ( NotificationMetadataEntity entity : metadataEntityList ) {
            metadataEntityMap.putIfAbsent(entity.getTableRow().get("iun"), entity);
        }
        metadataEntityMap.values().stream().sorted( Comparator.comparing( NotificationMetadataEntity::getSentAt ).reversed() )
                .forEach( entity -> result.add( entityToDto.entity2Dto( entity )) );
        log.debug( "END search for one month" );
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

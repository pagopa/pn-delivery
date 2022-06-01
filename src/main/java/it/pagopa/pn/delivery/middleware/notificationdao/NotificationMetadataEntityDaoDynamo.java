package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao {
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private EntityToDtoNotificationMetadataMapper entityToDto;
    private PnDataVaultClientImpl dataVaultClient;

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
        Instant startDate = inputSearchNotificationDto.getStartDate();
        Instant endDate = inputSearchNotificationDto.getEndDate();

        // costruzione delle Keys di ricerca in base alla partizione che si vuole interrogare ed al range di date di interesse
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

        // aggiunta dei filtri alla query: status, groups, iun
        addFilterExpression(inputSearchNotificationDto, requestBuilder);

        // se query su partizione precedente ha restituito una LEK
        // recupero nome dell'attributo in base all'indice di ricerca ed imposto
        // l'ultimo elemento valutato nella query precedente come exclusiveStartKey della query che segue
        if( lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty() ) {
            String attributeName = retrieveAttributeName( indexName );
            if ( lastEvaluatedKey.getInternalLastEvaluatedKey().get( attributeName ).s().equals( partitionValue ) ) {
                requestBuilder.exclusiveStartKey(lastEvaluatedKey.getInternalLastEvaluatedKey());
            }
        }

        // eseguo la query
        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( requestBuilder.build() );

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

    private void addFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                     QueryEnhancedRequest.Builder requestBuilder) {
        addStatusFilterExpression(inputSearchNotificationDto, requestBuilder);
        addGroupFilterExpression(inputSearchNotificationDto, requestBuilder);
        addIunFilterExpression( inputSearchNotificationDto, requestBuilder );
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

    private void addIunFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                        QueryEnhancedRequest.Builder requestBuilder) {
        if ( inputSearchNotificationDto.getIunMatch() != null ) {
            Expression filterIunExpression = Expression.builder()
                    .expression( "begins_with(iun_recipientId, :iunValue)" )
                    .putExpressionValue(
                            ":iunValue",
                            AttributeValue.builder()
                                    .s( inputSearchNotificationDto.getIunMatch() )
                                    .build()
                    ).build();
            requestBuilder.filterExpression( filterIunExpression );
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
        List<String> opaqueTaxIds = metadataEntityList.stream().map(NotificationMetadataEntity::getRecipientIds).flatMap( Collection::stream ).collect(Collectors.toList());
        if (!opaqueTaxIds.isEmpty()) {
            log.debug( "Opaque tax ids={}", opaqueTaxIds );
            List<BaseRecipientDto> dataVaultResults = dataVaultClient.getRecipientDenominationByInternalId( opaqueTaxIds );
            for (NotificationMetadataEntity entity : metadataEntityList) {
                Optional<BaseRecipientDto> match = dataVaultResults.stream().filter( r -> r.getInternalId().equals( entity.getRecipientId() ) ).findFirst();
                match.ifPresent(baseRecipientDto -> entity.setRecipientId(baseRecipientDto.getTaxId()));
                List<String> recipientTaxIds = new ArrayList<>();
                for (String recTaxId : entity.getRecipientIds() ) {
                    Optional<BaseRecipientDto> internalMatch = dataVaultResults.stream().filter( r -> r.getInternalId().equals(  recTaxId  ) ).findFirst();
                    internalMatch.ifPresent(baseRecipientDto -> recipientTaxIds.add(baseRecipientDto.getTaxId()));
                }
                entity.setRecipientIds( recipientTaxIds );
            }
        }
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

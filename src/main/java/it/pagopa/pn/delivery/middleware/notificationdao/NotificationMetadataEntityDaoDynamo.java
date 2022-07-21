package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao {

    protected NotificationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationMetadataEntity.class)));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationMetadataDao().getTableName();
    }


    @Override
    public PageSearchTrunk<NotificationMetadataEntity> searchByIun(
            InputSearchNotificationDto inputSearchNotificationDto,
            String partitionValue,
            String sentValue
    ) {
        log.debug( "START search for single IUN" );
        // costruzione delle Keys di ricerca in base alla partizione che si vuole interrogare ed al range di date di interesse

        GetItemEnhancedRequest.Builder requestBuilder = GetItemEnhancedRequest.builder();
        requestBuilder.key(Key.builder().partitionValue(partitionValue).sortValue(sentValue).build());


        log.debug( "START query execution" );
        // eseguo la query
        NotificationMetadataEntity entity = table.getItem( requestBuilder.build() );

        // applico i filtri
        // filtro per stato
        if (!CollectionUtils.isEmpty(inputSearchNotificationDto.getStatuses()) && !inputSearchNotificationDto.getStatuses().contains(NotificationStatus.fromValue(entity.getNotificationStatus())))
        {
            log.debug("result not satisfy filter status");
            return new PageSearchTrunk<>();
        }
        // filtro per gruppi
        if (!CollectionUtils.isEmpty(inputSearchNotificationDto.getGroups()) && !inputSearchNotificationDto.getGroups().contains(entity.getNotificationGroup()))
        {
            log.debug("result not satisfy filter groups");
            return new PageSearchTrunk<>();
        }
        // filtro per range date
        if (!entity.getSentAt().isAfter(inputSearchNotificationDto.getStartDate()) || !entity.getSentAt().isBefore(inputSearchNotificationDto.getEndDate()))
        {
            log.debug("result not satisfy filter dates");
            return  new PageSearchTrunk<>();
        }
        // filtro per mittente
        if (inputSearchNotificationDto.isBySender() && !entity.getSenderId().equals(inputSearchNotificationDto.getSenderReceiverId()) )
        {
            log.debug("result not satisfy filter sender");
            return  new PageSearchTrunk<>();
        }
        // filtro per destinatario
        if (!inputSearchNotificationDto.isBySender() && !entity.getRecipientIds().contains(inputSearchNotificationDto.getSenderReceiverId()) )
        {
            log.debug("result not satisfy filter receiver");
            return  new PageSearchTrunk<>();
        }
        // filtro per destinatario (su filterId, quindi a logica invertita rispetto ai 2 filtri precedenti)
        if (StringUtils.hasText(inputSearchNotificationDto.getFilterId()) && inputSearchNotificationDto.isBySender() && !entity.getRecipientIds().contains(inputSearchNotificationDto.getFilterId()) )
        {
            log.debug("result not satisfy filter filterid receiver");
            return  new PageSearchTrunk<>();
        }
        // filtro per mittente (su filterId, quindi a logica invertita rispetto ai 2 filtri precedenti)
        if (StringUtils.hasText(inputSearchNotificationDto.getFilterId()) && !inputSearchNotificationDto.isBySender() && entity.getSenderId().equals(inputSearchNotificationDto.getFilterId()) )
        {
            log.debug("result not satisfy filter filterid sender");
            return  new PageSearchTrunk<>();
        }

        // preparo i risultati
        PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
        res.setResults(List.of(entity));

        log.debug( "END query execution" );

        return res;
    }

    @Override
    public PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    ) {
        log.debug( "START search for one month" );
        Instant startDate = inputSearchNotificationDto.getStartDate();
        Instant endDate = inputSearchNotificationDto.getEndDate();

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
        addFilterExpression(inputSearchNotificationDto, requestBuilder);
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

        PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
        res.setResults( page.items() );
        res.setLastEvaluatedKey(page.lastEvaluatedKey());

        log.debug( "END mapper from metadata to searchRow" );
        return res;
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
                                        QueryEnhancedRequest.Builder requestBuilder
    ) {
        addRecipientOneFilterExpression( inputSearchNotificationDto, requestBuilder );
        addStatusFilterExpression( inputSearchNotificationDto.getStatuses(), requestBuilder);
        addGroupFilterExpression( inputSearchNotificationDto.getGroups(), requestBuilder);
    }

    private void addRecipientOneFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                                 QueryEnhancedRequest.Builder requestBuilder) {

        // nel caso in cui sono il mittente e sto cercando senza specificare il destinatario, applico il filtro su recipientOne (così mi torna solo il un record per iun multidestinatario)
        if (inputSearchNotificationDto.isBySender() && !StringUtils.hasText(inputSearchNotificationDto.getFilterId())) {
            Expression.Builder filterStatusExpressionBuilder = Expression.builder();

            filterStatusExpressionBuilder.putExpressionValue(":recipientOne",
                    AttributeValue.builder()
                            .bool(Boolean.TRUE)
                            .build());

            filterStatusExpressionBuilder.expression(NotificationMetadataEntity.FIELD_RECIPIENT_ONE + " = :recipientOne");
            requestBuilder.filterExpression(filterStatusExpressionBuilder.build());
        }
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

    @Override
    public void putIfAbsent(NotificationMetadataEntity notificationMetadataEntity) {
        PutItemEnhancedRequest<NotificationMetadataEntity> request = PutItemEnhancedRequest.
                builder(NotificationMetadataEntity.class)
                .item( notificationMetadataEntity )
                .build();
        table.putItem( request );
    }
}

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
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME;

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
        requestBuilder.key( k -> k.partitionValue(partitionValue)
                .sortValue(sentValue)
                .build()
        );


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
        log.trace( "START search for one month" );
        Instant startDate = inputSearchNotificationDto.getStartDate();
        Instant endDate = inputSearchNotificationDto.getEndDate();

        log.trace( "Key building ..." );
        // costruzione delle Keys di ricerca in base alla partizione che si vuole interrogare ed al range di date di interesse
        Key.Builder builder = Key.builder().partitionValue(partitionValue);
        Key key1 = builder.sortValue(startDate.toString()).build();
        Key key2 = builder.sortValue(endDate.toString()).build();
        log.trace( " ... key building done " +
                "startKeyPartition={} startKeyRange={} endKeyPartition={} endKeyRange={}",
                key1.partitionKeyValue(), key1.sortKeyValue(),
                key2.partitionKeyValue(), key2.sortKeyValue()
            );

        log.trace( "Create query conditional" );
        QueryConditional betweenConditional = QueryConditional
                .sortBetween( key1, key2 );

        DynamoDbIndex<NotificationMetadataEntity> index = table.index( indexName );

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder();

        requestBuilder.queryConditional( betweenConditional )
                .limit( size )
                .scanIndexForward( false );

        log.trace( "START add filter expression" );
        // aggiunta dei filtri alla query: status, groups, iun
        addFilterExpression(inputSearchNotificationDto, requestBuilder);
        log.trace( "END add filter expression" );

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
        QueryEnhancedRequest queryEnhancedRequest = requestBuilder.build();

        log.info( "START query execution index={} exclusiveStartKey={} startKeyPartition/Range={} endKeyPartition/Range={} expression={} expressionValues={}",
                index.indexName(),
                queryEnhancedRequest.exclusiveStartKey(),
                key1.partitionKeyValue() + "/" + key1.sortKeyValue(),
                key2.partitionKeyValue() + "/" + key2.sortKeyValue(),
                queryEnhancedRequest.filterExpression().expression(),
                queryEnhancedRequest.filterExpression().expressionValues()  );

        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( queryEnhancedRequest );

        log.trace( "END query execution" );

        // recupero i risultati della query
        Page<NotificationMetadataEntity> page = notificationMetadataPages.iterator().next();

        // imposto i risultati della query mappandoli da NotificationMetadata a NotificationSearchRow

        PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
        res.setResults( page.items() );
        res.setLastEvaluatedKey(page.lastEvaluatedKey());

        log.debug( "END search for one month readRows={} lastEvaluatedKey={}", (page.items()==null?0:page.items().size()), page.lastEvaluatedKey());
        return res;
    }

    private String retrieveAttributeName(String indexName) {
        String attributeName;
        switch (indexName) {
            case NotificationMetadataEntity.FIELD_SENDER_ID -> attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_CREATION_MONTH;
            case NotificationMetadataEntity.FIELD_RECIPIENT_ID -> attributeName = NotificationMetadataEntity.FIELD_RECIPIENT_ID_CREATION_MONTH;
            case NotificationMetadataEntity.INDEX_SENDER_ID_RECIPIENT_ID -> attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_RECIPIENT_ID;
            default -> {
                String msg = String.format("Unable to retrieve attributeName by indexName=%s", indexName);
                log.error(msg);
                throw new PnInternalException(msg, ERROR_CODE_DELIVERY_UNSUPPORTED_INDEX_NAME);
            }
        }
        return attributeName;
    }

    private void addFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                        QueryEnhancedRequest.Builder requestBuilder
    ) {
        Expression.Builder filterExpressionBuilder = Expression.builder();
        StringBuilder expressionBuilder = new StringBuilder();
        addRecipientOneFilterExpression( inputSearchNotificationDto, filterExpressionBuilder, expressionBuilder );
        addStatusFilterExpression( inputSearchNotificationDto.getStatuses(), filterExpressionBuilder, expressionBuilder);
        addGroupFilterExpression( inputSearchNotificationDto.getGroups(), filterExpressionBuilder, expressionBuilder);
        addPaIdsFilterExpression( inputSearchNotificationDto.getMandateAllowedPaIds(), filterExpressionBuilder, expressionBuilder);

        requestBuilder.filterExpression(filterExpressionBuilder
                .expression(expressionBuilder.length() > 0 ? expressionBuilder.toString() : null)
                .build());
    }

    private void addRecipientOneFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                                 Expression.Builder filterExpressionBuilder,
                                                 StringBuilder expressionBuilder) {

        // nel caso in cui sono il mittente e sto cercando senza specificare il destinatario, applico il filtro su recipientOne (così mi torna solo il un record per iun multidestinatario)
        if (inputSearchNotificationDto.isBySender() && !StringUtils.hasText(inputSearchNotificationDto.getFilterId())) {

            filterExpressionBuilder.putExpressionValue(":recipientOne",
                    AttributeValue.builder()
                            .bool(Boolean.TRUE)
                            .build());

            expressionBuilder.append(NotificationMetadataEntity.FIELD_RECIPIENT_ONE + " = :recipientOne");
        }
    }


    private void addStatusFilterExpression(List<NotificationStatus> statuses,
                                           Expression.Builder filterExpressionBuilder,
                                           StringBuilder expressionBuilder) {
        if (!CollectionUtils.isEmpty(statuses)) {
            if (expressionBuilder.length() > 0)
                expressionBuilder.append( " AND ( " );
            else {
                expressionBuilder.append( " ( " );
            }

            for (int i = 0;i<statuses.size();i++) {
                NotificationStatus notificationStatus = statuses.get(i);
                expressionBuilder.append("notificationStatus = :notificationStatusValue");
                expressionBuilder.append(i).append(" ");
                if (i<statuses.size()-1)
                    expressionBuilder.append(" OR ");

                filterExpressionBuilder.putExpressionValue(":notificationStatusValue"+i,
                        AttributeValue.builder()
                                .s( notificationStatus.toString() )
                                .build());
            }
            expressionBuilder.append( " ) ");
        }
    }

    private void addGroupFilterExpression(List<String> groupList,
                                          Expression.Builder filterExpressionBuilder,
                                          StringBuilder expressionBuilder) {
        if ( !CollectionUtils.isEmpty( groupList )) {
            // restituire anche le notifiche con gruppo <stringa_vuota>
            groupList.add("");
            log.trace( "Add group filter expression" );
            if ( expressionBuilder.length() > 0 )
                expressionBuilder.append( " AND ( " );
            else {
                expressionBuilder.append( " ( " );
            }

            for (int i = 0; i < groupList.size(); i++) {
                String group = groupList.get( i );
                expressionBuilder.append( "notificationGroup = :notificationGroupValue" );
                expressionBuilder.append(i).append(" ");
                if ( i < groupList.size() -1 )
                    expressionBuilder.append( " OR " );

                filterExpressionBuilder.putExpressionValue(":notificationGroupValue"+i,
                        AttributeValue.builder()
                                .s( group )
                                .build());
            }

            expressionBuilder.append(" OR attribute_not_exists(notificationGroup) )");
        }
    }


    private void addPaIdsFilterExpression(List<String> mandateAllowedPaIds,
                                          Expression.Builder filterExpressionBuilder,
                                          StringBuilder expressionBuilder) {
        if ( !CollectionUtils.isEmpty( mandateAllowedPaIds )) {
            // devo restituire solo le righe con PaId mittente permessa nelle deleghe
            log.trace( "Add paIds filter expression" );
            if ( expressionBuilder.length() > 0 )
                expressionBuilder.append( " AND  ( " );
            else {
                expressionBuilder.append( " ( " );
            }

            for (int i = 0; i < mandateAllowedPaIds.size(); i++) {
                String paid = mandateAllowedPaIds.get( i );
                expressionBuilder.append( "senderId = :mandateAllowedPaId" );
                expressionBuilder.append(i).append(" ");
                if ( i < mandateAllowedPaIds.size() -1 )
                    expressionBuilder.append( " OR " );

                filterExpressionBuilder.putExpressionValue(":mandateAllowedPaId"+i,
                        AttributeValue.builder()
                                .s( paid )
                                .build());
            }


            expressionBuilder.append(" )");
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

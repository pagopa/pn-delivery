package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
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

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_GENERIC_ERROR;
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
        validateCommunicationType(inputSearchNotificationDto);
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

        if (entity == null)
        {
            log.debug("result entity is null");
            return new PageSearchTrunk<>();
        }

        // applico i filtri
        // filtro per stato
        if (!CollectionUtils.isEmpty(inputSearchNotificationDto.getStatuses()) && !inputSearchNotificationDto.getStatuses().contains(NotificationStatusV26.fromValue(entity.getNotificationStatus())))
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
        // filtro per destinatario (non applicabile al flusso campagna, che non ha un destinatario "di sessione")
        if (!inputSearchNotificationDto.isBySender() && !inputSearchNotificationDto.isByCampaign() && !entity.getRecipientIds().contains(inputSearchNotificationDto.getSenderReceiverId()) )
        {
            log.debug("result not satisfy filter receiver");
            return  new PageSearchTrunk<>();
        }
        // filtro per destinatario (su filterId, quindi a logica invertita rispetto ai 2 filtri precedenti)
        if (StringUtils.hasText(inputSearchNotificationDto.getFilterId())
            && (inputSearchNotificationDto.isBySender() || inputSearchNotificationDto.isByCampaign())
            && !(
            (StringUtils.hasText(inputSearchNotificationDto.getOpaqueFilterIdPF())
                && entity.getRecipientIds().contains(inputSearchNotificationDto.getOpaqueFilterIdPF()))
            || (StringUtils.hasText(inputSearchNotificationDto.getOpaqueFilterIdPG())
                && entity.getRecipientIds().contains(inputSearchNotificationDto.getOpaqueFilterIdPG()))
            ))
        {
            log.debug("result not satisfy filter filterid receiver");
            return  new PageSearchTrunk<>();
        }
        // filtro per mittente (su filterId, quindi a logica invertita rispetto ai 2 filtri precedenti)
        if (StringUtils.hasText(inputSearchNotificationDto.getFilterId()) && !inputSearchNotificationDto.isBySender() && !inputSearchNotificationDto.isByCampaign() && !entity.getSenderId().equals(inputSearchNotificationDto.getFilterId()) )
        {
            log.debug("result not satisfy filter filterid sender");
            return  new PageSearchTrunk<>();
        }
        // filtro per mittente gruppo
        if( (inputSearchNotificationDto.isBySender() || inputSearchNotificationDto.isByCampaign())
                && !CollectionUtils.isEmpty( inputSearchNotificationDto.getGroups())
                && !inputSearchNotificationDto.getGroups().contains( entity.getNotificationGroup() ) ) {
            log.debug("result not satisfy filter group");
            return new PageSearchTrunk<>();
        }

        // filtro per tipologia di comunicazione (trattandosi di GetItem, applicato in memoria con la stessa semantica della query)
        if ( !matchesCommunicationTypeFilter( inputSearchNotificationDto.getCommunicationType(), entity ) ) {
            log.debug("result not satisfy filter communicationType");
            return new PageSearchTrunk<>();
        }
        // filtro per appartenenza alla campagna (ricerca puntuale per IUN nel flusso bonarie):
        // scarto l'entity se non appartiene alla campagna richiesta
        if ( inputSearchNotificationDto.isByCampaign() && !inputSearchNotificationDto.getCampaignId().equals( entity.getCampaignId() ) ) {
            log.debug("result not satisfy filter campaign");
            return new PageSearchTrunk<>();
        }
        // filtro per stato bonario (flusso campagna), applicato in memoria con la stessa semantica della query
        if ( !matchesInformalStatusFilter( inputSearchNotificationDto.getInformalStatuses(), entity ) ) {
            log.debug("result not satisfy filter informal status");
            return new PageSearchTrunk<>();
        }
        // filtro per esito (viewed/delivered), applicato in memoria con la stessa semantica della query
        if ( !matchesEsitoFilter( inputSearchNotificationDto, entity ) ) {
            log.debug("result not satisfy filter esito");
            return new PageSearchTrunk<>();
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
        validateCommunicationType(inputSearchNotificationDto);
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

        log.trace( "START query execution index={}", index.indexName()  );

        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( queryEnhancedRequest );

        log.trace( "END query execution" );

        // recupero i risultati della query
        Page<NotificationMetadataEntity> page = notificationMetadataPages.iterator().next();

        // imposto i risultati della query mappandoli da NotificationMetadata a NotificationSearchRow

        PageSearchTrunk<NotificationMetadataEntity> res = new PageSearchTrunk<>();
        res.setResults( page.items() );
        res.setLastEvaluatedKey(page.lastEvaluatedKey());

        log.info( "DONE search for one month index={} requiredSize={} exclusiveStartKey={} startKeyPartition/Range={} endKeyPartition/Range={} expression={} expressionValues={} readRows={} lastEvaluatedKey={}",
                index.indexName(),
                queryEnhancedRequest.limit(),
                queryEnhancedRequest.exclusiveStartKey(),
                key1.partitionKeyValue() + "/" + key1.sortKeyValue(),
                key2.partitionKeyValue() + "/" + key2.sortKeyValue(),
                queryEnhancedRequest.filterExpression()==null?null:queryEnhancedRequest.filterExpression().expression(),
                queryEnhancedRequest.filterExpression()==null?null:queryEnhancedRequest.filterExpression().expressionValues(),
                (page.items()==null?0:page.items().size()),
                page.lastEvaluatedKey() );
        return res;
    }

    /**
     * Filtro in memoria per tipologia di comunicazione, usato dalla ricerca puntuale per IUN ({@code GetItem}).
     * Stessa semantica della query multi-mese:
     * <ul>
        *     <li>{@code LEGAL}: {@code communicationType == LEGAL} oppure {@code null} (notifiche storiche);</li>
     *     <li>{@code INFORMAL}: {@code communicationType == INFORMAL};</li>
     *     <li>{@code ALL}: nessun filtro.</li>
     * </ul>
     */
    private boolean matchesCommunicationTypeFilter(NotificationSearchCommunicationType communicationType,
                                                   NotificationMetadataEntity entity) {
        // ALL: nessun filtro
        if (communicationType == NotificationSearchCommunicationType.ALL) {
            return true;
        }

        String entityCommunicationType = entity.getCommunicationType();
        if (communicationType == NotificationSearchCommunicationType.LEGAL) {
            // include le notifiche storiche prive del campo communicationType
            return !StringUtils.hasText(entityCommunicationType)
                    || NotificationSearchCommunicationType.LEGAL.name().equals(entityCommunicationType);
        }
        // INFORMAL
        return NotificationSearchCommunicationType.INFORMAL.name().equals(entityCommunicationType);
    }

    private void validateCommunicationType(InputSearchNotificationDto inputSearchNotificationDto) {
        if (inputSearchNotificationDto.getCommunicationType() == null) {
            throw new PnInternalException(
                    "communicationType is required for notification metadata search",
                    ERROR_CODE_DELIVERY_GENERIC_ERROR);
        }
    }

    /**
     * Filtro in memoria per esito (viewed/delivered), usato dalla ricerca puntuale per IUN ({@code GetItem}).
     * Stessa semantica della query: il filtro viene applicato solo se il relativo flag &egrave; valorizzato.
     */
    private boolean matchesEsitoFilter(InputSearchNotificationDto inputSearchNotificationDto,
                                       NotificationMetadataEntity entity) {
        Boolean viewed = inputSearchNotificationDto.getViewed();
        if (viewed != null && !viewed.equals(entity.getViewed())) {
            return false;
        }
        Boolean delivered = inputSearchNotificationDto.getDelivered();
        return delivered == null || delivered.equals(entity.getDelivered());
    }

    /**
     * Filtro in memoria per stato bonario, usato dalla ricerca puntuale per IUN ({@code GetItem})
     * nel flusso campagna. Lo stato &egrave; confrontato per valore stringa con
     * {@code entity.notificationStatus}. Se nessuno stato bonario &egrave; richiesto, nessun filtro.
     */
    private boolean matchesInformalStatusFilter(List<InformalNotificationStatusV1> informalStatuses,
                                                NotificationMetadataEntity entity) {
        if (CollectionUtils.isEmpty(informalStatuses)) {
            return true;
        }
        return informalStatuses.stream()
                .anyMatch(s -> s.getValue().equals(entity.getNotificationStatus()));
    }

    private String retrieveAttributeName(String indexName) {
        String attributeName;
        switch (indexName) {
            case NotificationMetadataEntity.FIELD_SENDER_ID -> attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_CREATION_MONTH;
            case NotificationMetadataEntity.FIELD_RECIPIENT_ID -> attributeName = NotificationMetadataEntity.FIELD_RECIPIENT_ID_CREATION_MONTH;
            case NotificationMetadataEntity.INDEX_SENDER_ID_RECIPIENT_ID -> attributeName = NotificationMetadataEntity.FIELD_SENDER_ID_RECIPIENT_ID;
            case NotificationMetadataEntity.INDEX_BY_CAMPAIGN -> attributeName = NotificationMetadataEntity.FIELD_CAMPAIGN_ID_CREATION_MONTH;
            case NotificationMetadataEntity.INDEX_BY_CAMPAIGN_RECIPIENT -> attributeName = NotificationMetadataEntity.FIELD_CAMPAIGN_ID_RECIPIENT_ID;
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
        addInformalStatusFilterExpression( inputSearchNotificationDto.getInformalStatuses(), filterExpressionBuilder, expressionBuilder);
        addGroupFilterExpression( inputSearchNotificationDto.getGroups(), filterExpressionBuilder, expressionBuilder);
        addPaIdsFilterExpression( inputSearchNotificationDto.getMandateAllowedPaIds(), filterExpressionBuilder, expressionBuilder);
        addCommunicationTypeFilterExpression( inputSearchNotificationDto.getCommunicationType(), filterExpressionBuilder, expressionBuilder);
        addEsitoFilterExpression( inputSearchNotificationDto, filterExpressionBuilder, expressionBuilder);

        requestBuilder.filterExpression(filterExpressionBuilder
                .expression(expressionBuilder.length() > 0 ? expressionBuilder.toString() : null)
                .build());
    }

    private void addRecipientOneFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                                 Expression.Builder filterExpressionBuilder,
                                                 StringBuilder expressionBuilder) {

        // nel caso in cui sono il mittente (o sto eseguendo una ricerca massiva per campagna utile se in futuro ci saranno i multidestinatari)
        // e sto cercando senza specificare il destinatario, applico il filtro su recipientOne (così mi torna solo un record per iun multidestinatario)
        boolean massiveSenderSearch = inputSearchNotificationDto.isBySender() && !StringUtils.hasText(inputSearchNotificationDto.getFilterId());
        boolean massiveCampaignSearch = inputSearchNotificationDto.isByCampaign() && !StringUtils.hasText(inputSearchNotificationDto.getFilterId());
        if (massiveSenderSearch || massiveCampaignSearch) {

            filterExpressionBuilder.putExpressionValue(":recipientOne",
                    AttributeValue.builder()
                            .bool(Boolean.TRUE)
                            .build());

            expressionBuilder.append(NotificationMetadataEntity.FIELD_RECIPIENT_ONE + " = :recipientOne");
        }
    }


    private void addStatusFilterExpression(List<NotificationStatusV26> statuses,
                                           Expression.Builder filterExpressionBuilder,
                                           StringBuilder expressionBuilder) {
        if (!CollectionUtils.isEmpty(statuses)) {
            if (expressionBuilder.length() > 0)
                expressionBuilder.append( " AND ( " );
            else {
                expressionBuilder.append( " ( " );
            }

            for (int i = 0;i<statuses.size();i++) {
                NotificationStatusV26 notificationStatus = statuses.get(i);
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

    private void addInformalStatusFilterExpression(List<InformalNotificationStatusV1> informalStatuses,
                                                   Expression.Builder filterExpressionBuilder,
                                                   StringBuilder expressionBuilder) {
        if (!CollectionUtils.isEmpty(informalStatuses)) {
            if (expressionBuilder.length() > 0)
                expressionBuilder.append( " AND ( " );
            else {
                expressionBuilder.append( " ( " );
            }

            for (int i = 0; i < informalStatuses.size(); i++) {
                InformalNotificationStatusV1 informalStatus = informalStatuses.get(i);
                expressionBuilder.append(NotificationMetadataEntity.FIELD_NOTIFICATION_STATUS + " = :informalStatusValue");
                expressionBuilder.append(i).append(" ");
                if (i < informalStatuses.size() - 1)
                    expressionBuilder.append(" OR ");

                filterExpressionBuilder.putExpressionValue(":informalStatusValue" + i,
                        AttributeValue.builder()
                                .s( informalStatus.getValue() )
                                .build());
            }
            expressionBuilder.append( " ) ");
        }
    }

    private void addGroupFilterExpression(List<String> groupList,
                                          Expression.Builder filterExpressionBuilder,
                                          StringBuilder expressionBuilder) {
        // se l'utente appartiene ad almeno 1 gruppo, deve poter vedere SOLO le notifiche di quei gruppi
        // se invece non appartiene a gruppi, vede tutto (quelle con gruppo e quelle senza)
        if ( !CollectionUtils.isEmpty( groupList )) {

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

            expressionBuilder.append(" )");

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
                expressionBuilder.append( "rootSenderId = :mandateAllowedPaId" );
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

    /**
     * Filtro per tipologia di comunicazione applicato alla query multi-mese.
     * <ul>
        *     <li>{@code LEGAL}:
     *     {@code (communicationType = :legal OR attribute_not_exists(communicationType))},
     *     così da includere anche le notifiche storiche precedenti al rilascio prive del campo;</li>
     *     <li>{@code INFORMAL}: {@code communicationType = :informal};</li>
     *     <li>{@code ALL}: nessun filtro (entrambe le tipologie).</li>
     * </ul>
     * Lo stesso metodo è riutilizzabile dal flusso mittente per escludere le bonarie.
     */
    private void addCommunicationTypeFilterExpression(NotificationSearchCommunicationType communicationType,
                                                      Expression.Builder filterExpressionBuilder,
                                                      StringBuilder expressionBuilder) {
        // ALL: nessun filtro
        if (communicationType == NotificationSearchCommunicationType.ALL) {
            return;
        }

        log.trace( "Add communicationType filter expression communicationType={}", communicationType );
        if ( expressionBuilder.length() > 0 )
            expressionBuilder.append( " AND ( " );
        else {
            expressionBuilder.append( " ( " );
        }

        if (communicationType == NotificationSearchCommunicationType.LEGAL) {
            // includo anche le notifiche storiche prive del campo communicationType
            expressionBuilder.append( NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :legal" )
                    .append( " OR attribute_not_exists(" + NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + ")" );
            filterExpressionBuilder.putExpressionValue(":legal",
                    AttributeValue.builder()
                            .s( NotificationSearchCommunicationType.LEGAL.name() )
                            .build());
        } else {
            expressionBuilder.append( NotificationMetadataEntity.FIELD_COMMUNICATION_TYPE + " = :informal" );
            filterExpressionBuilder.putExpressionValue(":informal",
                    AttributeValue.builder()
                            .s( NotificationSearchCommunicationType.INFORMAL.name() )
                            .build());
        }

        expressionBuilder.append(" )");
    }

    /**
     * Filtro per esito applicato alla query multi-mese. I flag {@code viewed} e {@code delivered}
     * sono indipendenti: ciascuno viene aggiunto come condizione {@code = :flag} solo se valorizzato.
     * Se entrambi assenti, nessun filtro viene applicato.
     */
    private void addEsitoFilterExpression(InputSearchNotificationDto inputSearchNotificationDto,
                                          Expression.Builder filterExpressionBuilder,
                                          StringBuilder expressionBuilder) {
        Boolean viewed = inputSearchNotificationDto.getViewed();
        if (viewed != null) {
            log.trace( "Add viewed filter expression viewed={}", viewed );
            if ( expressionBuilder.length() > 0 )
                expressionBuilder.append( " AND ( " );
            else
                expressionBuilder.append( " ( " );
            expressionBuilder.append( NotificationMetadataEntity.FIELD_VIEWED + " = :viewed )" );
            filterExpressionBuilder.putExpressionValue(":viewed",
                    AttributeValue.builder().bool(viewed).build());
        }

        Boolean delivered = inputSearchNotificationDto.getDelivered();
        if (delivered != null) {
            log.trace( "Add delivered filter expression delivered={}", delivered );
            if ( expressionBuilder.length() > 0 )
                expressionBuilder.append( " AND ( " );
            else
                expressionBuilder.append( " ( " );
            expressionBuilder.append( NotificationMetadataEntity.FIELD_DELIVERED + " = :delivered )" );
            filterExpressionBuilder.putExpressionValue(":delivered",
                    AttributeValue.builder().bool(delivered).build());
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

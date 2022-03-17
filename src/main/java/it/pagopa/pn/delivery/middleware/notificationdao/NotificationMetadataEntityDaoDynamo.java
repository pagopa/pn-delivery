package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.middleware.model.notification.NotificationMetadataEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationMetadataEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationMetadataEntity> implements NotificationMetadataEntityDao<Key, NotificationMetadataEntity> {
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private EntityToDtoNotificationMetadataMapper entityToDto;

    protected NotificationMetadataEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, EntityToDtoNotificationMetadataMapper entityToDto) {
        super(dynamoDbEnhancedClient.table(NotificationMetadataEntity.NOTIFICATIONS_METADATA_TABLE_NAME, TableSchema.fromClass(NotificationMetadataEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.entityToDto = entityToDto;
    }

    @Override
    public List<NotificationSearchRow> searchNotificationMetadata(InputSearchNotificationDto inputSearchNotificationDto) {
        final String startDate = inputSearchNotificationDto.getStartDate().toString();
        final String endDate = inputSearchNotificationDto.getEndDate().toString();
        List<String> creationMonths = retrieveCreationMonth( startDate, endDate );

        String indexName;
        String partitionValue;
        if(inputSearchNotificationDto.isBySender()) {
            indexName = "senderId";
            if (inputSearchNotificationDto.getFilterId() != null) {
                indexName += "_recipientId";
                partitionValue = inputSearchNotificationDto.getSenderReceiverId()
                        + "##" + inputSearchNotificationDto.getFilterId();
            } else {
                partitionValue = inputSearchNotificationDto.getSenderReceiverId()
                        + "##" + creationMonths.get(0); //TODO prevedere gestione partition value multipli per ricerche di più mesi
            }
        } else {
            indexName = "recipientId";
            if (inputSearchNotificationDto.getFilterId() != null) {
                indexName = "senderId_" + indexName;
                partitionValue = inputSearchNotificationDto.getSenderReceiverId()
                        + "##" + inputSearchNotificationDto.getFilterId();
            } else {
                partitionValue = inputSearchNotificationDto.getSenderReceiverId()
                        + "##" + creationMonths.get(0); //TODO prevedere gestione partition value multipli per ricerche di più mesi
            }
        }

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
        requestBuilder.queryConditional( queryConditional ).queryConditional( betweenConditional );

        addFilterExpression(inputSearchNotificationDto, requestBuilder);

        List<NotificationMetadataEntity> metadataEntityList = new ArrayList<>();
        SdkIterable<Page<NotificationMetadataEntity>> notificationMetadataPages = index.query( requestBuilder.build() );
        notificationMetadataPages.stream().forEach( pages -> metadataEntityList.addAll(pages.items()) );

        return fromNotificationMetadataToNotificationSearchRow( metadataEntityList );
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

    private List<String> retrieveCreationMonth(String startDate, String endDate) {
        List<String> creationMonths = new ArrayList<>();
        String[] splitStartDate = startDate.split( "-" );
        String[] splitEndDate = endDate.split( "-" );
        String startCreationMonth = splitStartDate[0] + splitStartDate[1];
        String endCreationMonth = splitEndDate[0] + splitEndDate[1];
        if (startCreationMonth.equals( endCreationMonth ))
        {
            creationMonths.add( startCreationMonth );
        } else {
            // TODO scorrere mese da start ad end date ed aggiungerlo alla lista dei creationMonths
        }
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

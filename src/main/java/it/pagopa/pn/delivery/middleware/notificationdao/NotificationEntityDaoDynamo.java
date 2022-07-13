package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class NotificationEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationEntity> implements NotificationEntityDao {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<NotificationEntity> dynamoDbTable;
    private final DynamoDbTable<NotificationCostEntity> dynamoDbCostTable;
    private final EntityToDtoNotificationMapper entity2DtoMapper;

    public NotificationEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg, EntityToDtoNotificationMapper entity2DtoMapper) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.entity2DtoMapper = entity2DtoMapper;
        this.dynamoDbTable = dynamoDbEnhancedClient.table(tableName( cfg), TableSchema.fromClass(NotificationEntity.class));
        this.dynamoDbCostTable = dynamoDbEnhancedClient.table( costTableName(cfg), TableSchema.fromClass(NotificationCostEntity.class));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationDao().getTableName();
    }
    private static String costTableName(PnDeliveryConfigs cfg ) {
        return cfg.getNotificationCostDao().getTableName();
    }

    @Override
    public void putIfAbsent(NotificationEntity notificationEntity) throws IdConflictException {
        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(iun)")
                .build();

        PutItemEnhancedRequest<NotificationEntity> request1 = PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( notificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build();

        NotificationEntity controlNotificationEntity = NotificationEntity.builder()
                .iun( getControlIun(notificationEntity) )
                .build();

        List<NotificationCostEntity> notificationCostEntityList = getNotificationCostEntities( notificationEntity );

        List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList = createPutItemRequests( notificationCostEntityList );

        PutItemEnhancedRequest<NotificationEntity> request2 = PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( controlNotificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build();

        TransactWriteItemsEnhancedRequest enhancedRequest = createTransactWriteItems( request1, request2, costRequestList );

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            Map<String,String> duplicatedErrors = getDuplicationErrors(notificationEntity, notificationCostEntityList);
            throw new IdConflictException( duplicatedErrors );
        }
    }

    @NotNull
    private Map<String,String> getDuplicationErrors(NotificationEntity notificationEntity, List<NotificationCostEntity> notificationCostEntityList) {
        NotificationEntity iunDuplicated = dynamoDbTable.getItem( Key.builder()
                        .partitionValue( notificationEntity.getIun() )
                .build() );
        String controlIun = getControlIun(notificationEntity);
        NotificationEntity paProtocolDuplicated = dynamoDbTable.getItem( Key.builder()
                        .partitionValue( controlIun )
                .build() );
        List<NotificationCostEntity> costEntitiesDuplicated = new ArrayList<>();
        for ( NotificationCostEntity notificationCostEntity : notificationCostEntityList) {
            NotificationCostEntity costEntityDuplicated = dynamoDbCostTable.getItem( Key.builder()
                    .partitionValue( notificationCostEntity.getCreditorTaxId_noticeCode() )
                    .build());
            if ( costEntityDuplicated != null ) {
                costEntitiesDuplicated.add( costEntityDuplicated );
            }
        }
        Map<String,String> duplicatedErrors = new HashMap<>();
        if ( iunDuplicated != null ) {
            duplicatedErrors.put( "iun", notificationEntity.getIun() );
        }
        if ( paProtocolDuplicated != null ) {
            duplicatedErrors.put("senderPaId##paProtocolNumber##cancelledIun" , controlIun );
        }
        for ( NotificationCostEntity nce : costEntitiesDuplicated ) {
            duplicatedErrors.put("creditorTaxId##noticeCode", nce.getCreditorTaxId_noticeCode());
        }
        return duplicatedErrors;
    }

    private TransactWriteItemsEnhancedRequest createTransactWriteItems(PutItemEnhancedRequest<NotificationEntity> request1, PutItemEnhancedRequest<NotificationEntity> request2, List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList) {
        TransactWriteItemsEnhancedRequest.Builder requestBuilder = TransactWriteItemsEnhancedRequest.builder();
        requestBuilder.addPutItem( dynamoDbTable, request1 );
        requestBuilder.addPutItem( dynamoDbTable, request2 );
        for (PutItemEnhancedRequest<NotificationCostEntity> putItemCost : costRequestList  ) {
            requestBuilder.addPutItem( dynamoDbCostTable, putItemCost );
        }
        return requestBuilder.build();
    }

    private List<PutItemEnhancedRequest<NotificationCostEntity>> createPutItemRequests(List<NotificationCostEntity> notificationCostEntityList) {
        List<PutItemEnhancedRequest<NotificationCostEntity>> putItemEnhancedRequestList = new ArrayList<>();
        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(creditorTaxId_noticeCode)")
                .build();
        for ( NotificationCostEntity costEntity : notificationCostEntityList ) {
            putItemEnhancedRequestList.add( PutItemEnhancedRequest.builder( NotificationCostEntity.class )
                    .item( costEntity )
                    .conditionExpression( conditionExpressionPut )
                    .build()
            );
        }
        return putItemEnhancedRequestList;
    }

    private List<NotificationCostEntity> getNotificationCostEntities(NotificationEntity notificationEntity) {
        List<NotificationCostEntity> notificationCostEntityList = new ArrayList<>();

        for (NotificationRecipientEntity rec : notificationEntity.getRecipients() ) {
            notificationCostEntityList.add( NotificationCostEntity.builder()
                    .recipientIdx( notificationEntity.getRecipients().indexOf( rec ) )
                    .iun( notificationEntity.getIun() )
                    .creditorTaxId_noticeCode( rec.getPayment().getCreditorTaxId() + "##" + rec.getPayment().getNoticeCode() )
                    .build()
            );
            if ( rec.getPayment().getNoticeCodeAlternative() != null ) {
                notificationCostEntityList.add( NotificationCostEntity.builder()
                        .recipientIdx( notificationEntity.getRecipients().indexOf( rec ) )
                        .iun( notificationEntity.getIun() )
                        .creditorTaxId_noticeCode( rec.getPayment().getCreditorTaxId() + "##" + rec.getPayment().getNoticeCodeAlternative() )
                        .build()
                );
            }
        }
        return notificationCostEntityList;
    }

    @NotNull
    private String getControlIun(NotificationEntity notificationEntity) {
        return notificationEntity.getSenderPaId()
                + "##" + notificationEntity.getPaNotificationId()
                + "##" + notificationEntity.getCancelledIun();
    }

}

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

import java.util.*;

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
        List<PutItemEnhancedRequest<NotificationEntity>> notificationRequestList = createNotificationPutItemRequests( notificationEntity );

        List<NotificationCostEntity> notificationCostEntityList = getNotificationCostEntities( notificationEntity );

        List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList = createCostPutItemRequests( notificationCostEntityList );

        TransactWriteItemsEnhancedRequest enhancedRequest = createTransactWriteItems( notificationRequestList, costRequestList );

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            Map<String,String> duplicatedErrors = getDuplicationErrors(notificationEntity, notificationCostEntityList);
            throw new IdConflictException( duplicatedErrors );
        }
    }

    private List<PutItemEnhancedRequest<NotificationEntity>> createNotificationPutItemRequests(NotificationEntity notificationEntity) {
        List<PutItemEnhancedRequest<NotificationEntity>> notificationRequestList = new ArrayList<>();

        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(iun)")
                .build();

        notificationRequestList.add( PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( notificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build()
        );

        NotificationEntity controlCancelledIun = NotificationEntity.builder()
                .iun( getControlCancelledIun(notificationEntity) )
                .build();

        notificationRequestList.add( PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( controlCancelledIun )
                .conditionExpression( conditionExpressionPut )
                .build()
        );

        NotificationEntity controlIdempotenceToken = NotificationEntity.builder()
                .iun( getControlIdempotenceToken(notificationEntity) )
                .build();

        notificationRequestList.add( PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( controlIdempotenceToken )
                .conditionExpression( conditionExpressionPut )
                .build()
        );

        return notificationRequestList;
    }

    private String getControlIdempotenceToken(NotificationEntity notificationEntity) {
        return notificationEntity.getSenderPaId()
                + "##" + notificationEntity.getPaNotificationId()
                + "##" + notificationEntity.getIdempotenceToken();
    }

    @NotNull
    private Map<String,String> getDuplicationErrors(NotificationEntity notificationEntity, List<NotificationCostEntity> notificationCostEntityList) {
        NotificationEntity iunDuplicated = dynamoDbTable.getItem( Key.builder()
                        .partitionValue( notificationEntity.getIun() )
                .build() );

        String controlCancelledIun = getControlCancelledIun(notificationEntity);
        NotificationEntity cancelledIunDuplicated = dynamoDbTable.getItem( Key.builder()
                        .partitionValue( controlCancelledIun )
                .build() );

        String controlIdempotenceToken = getControlIdempotenceToken( notificationEntity );
        NotificationEntity idempotenceTokenDuplicated = dynamoDbTable.getItem( Key.builder()
                        .partitionValue( controlIdempotenceToken )
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
        if ( Objects.nonNull( iunDuplicated ) ) {
            duplicatedErrors.put( "iun", notificationEntity.getIun() );
        }
        if ( Objects.nonNull( cancelledIunDuplicated ) ) {
            duplicatedErrors.put("senderPaId##paProtocolNumber##cancelledIun" , controlCancelledIun );
        }
        if ( Objects.nonNull( idempotenceTokenDuplicated ) ) {
            duplicatedErrors.put("senderPaId##paProtocolNumber##idempotenceToken" , controlIdempotenceToken );
        }
        for ( NotificationCostEntity nce : costEntitiesDuplicated ) {
            duplicatedErrors.put("creditorTaxId##noticeCode", nce.getCreditorTaxId_noticeCode());
        }
        return duplicatedErrors;
    }

    private TransactWriteItemsEnhancedRequest createTransactWriteItems(List<PutItemEnhancedRequest<NotificationEntity>> notificationRequestList, List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList) {
        TransactWriteItemsEnhancedRequest.Builder requestBuilder = TransactWriteItemsEnhancedRequest.builder();

        for ( PutItemEnhancedRequest<NotificationEntity> putItemNotification: notificationRequestList ) {
            requestBuilder.addPutItem( dynamoDbTable, putItemNotification);
        }
        for (PutItemEnhancedRequest<NotificationCostEntity> putItemCost : costRequestList  ) {
            requestBuilder.addPutItem( dynamoDbCostTable, putItemCost );
        }
        return requestBuilder.build();
    }

    private List<PutItemEnhancedRequest<NotificationCostEntity>> createCostPutItemRequests(List<NotificationCostEntity> notificationCostEntityList) {
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
    private String getControlCancelledIun(NotificationEntity notificationEntity) {
        return notificationEntity.getSenderPaId()
                + "##" + notificationEntity.getPaNotificationId()
                + "##" + notificationEntity.getCancelledIun();
    }

}

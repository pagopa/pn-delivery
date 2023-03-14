package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class NotificationEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationEntity> implements NotificationEntityDao {
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<NotificationEntity> dynamoDbTable;
    private final DynamoDbTable<NotificationCostEntity> dynamoDbCostTable;
    private final DynamoDbTable<NotificationQREntity> dynamoDbQRTable;

    public NotificationEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.dynamoDbTable = dynamoDbEnhancedClient.table(tableName( cfg), TableSchema.fromClass(NotificationEntity.class));
        this.dynamoDbCostTable = dynamoDbEnhancedClient.table( costTableName(cfg), TableSchema.fromClass(NotificationCostEntity.class));
        this.dynamoDbQRTable = dynamoDbEnhancedClient.table( qrTableName(cfg), TableSchema.fromClass(NotificationQREntity.class));
    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationDao().getTableName();
    }
    private static String costTableName(PnDeliveryConfigs cfg ) {
        return cfg.getNotificationCostDao().getTableName();
    }
    private static String qrTableName(PnDeliveryConfigs cfg) { return cfg.getNotificationQRDao().getTableName(); }

    @Override
    public void putIfAbsent(NotificationEntity notificationEntity) throws PnIdConflictException {
        List<TransactPutItemEnhancedRequest<NotificationEntity>> notificationRequestList = createNotificationPutItemRequests( notificationEntity );

        List<NotificationCostEntity> notificationCostEntityList = getNotificationCostEntities( notificationEntity );

        List<NotificationQREntity> notificationQREntityList = getNotificationQREntities( notificationEntity );

        List<TransactPutItemEnhancedRequest<NotificationCostEntity>> costRequestList = createCostPutItemRequests( notificationCostEntityList );

        List<TransactPutItemEnhancedRequest<NotificationQREntity>> qrRequestList = createQRPutItemRequests( notificationQREntityList );

        TransactWriteItemsEnhancedRequest enhancedRequest = createTransactWriteItems( notificationRequestList, costRequestList, qrRequestList );

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            Map<String,String> duplicatedErrors = getDuplicationErrors(notificationEntity, notificationCostEntityList);
            throw new PnIdConflictException( duplicatedErrors );
        }
    }

    private List<TransactPutItemEnhancedRequest<NotificationEntity>> createNotificationPutItemRequests(NotificationEntity notificationEntity) {
        List<TransactPutItemEnhancedRequest<NotificationEntity>> notificationRequestList = new ArrayList<>();

        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(iun)")
                .build();

        notificationRequestList.add( TransactPutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( notificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build()
        );


        NotificationEntity controlIdempotenceToken = NotificationEntity.builder()
                .iun( getControlIdempotenceToken(notificationEntity) )
                .requestId( Base64Utils.encodeToString( notificationEntity.getIun().getBytes(StandardCharsets.UTF_8) ) )
                .build();

        notificationRequestList.add( TransactPutItemEnhancedRequest.builder( NotificationEntity.class )
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


        String controlIdempotenceToken = getControlIdempotenceToken( notificationEntity );
        NotificationEntity idempotenceTokenDuplicated = dynamoDbTable.getItem( Key.builder()
                .partitionValue( controlIdempotenceToken )
                .build() );

        List<NotificationCostEntity> costEntitiesDuplicated = new ArrayList<>();
        for ( NotificationCostEntity notificationCostEntity : notificationCostEntityList) {
            NotificationCostEntity costEntityDuplicated = dynamoDbCostTable.getItem( Key.builder()
                    .partitionValue( notificationCostEntity.getCreditorTaxIdNoticeCode() )
                    .build());
            if ( costEntityDuplicated != null ) {
                costEntitiesDuplicated.add( costEntityDuplicated );
            }
        }
        Map<String,String> duplicatedErrors = new HashMap<>();
        if ( Objects.nonNull( iunDuplicated ) ) {
            duplicatedErrors.put( "Duplicated notification for iun", notificationEntity.getIun() );
        }
        if ( Objects.nonNull( idempotenceTokenDuplicated ) ) {
            duplicatedErrors.put("Duplicated notification for senderPaId##paProtocolNumber##idempotenceToken" , controlIdempotenceToken );
        }
        for ( NotificationCostEntity nce : costEntitiesDuplicated ) {
            duplicatedErrors.put("Duplicated notification for creditorTaxId##noticeCode", nce.getCreditorTaxIdNoticeCode());
        }
        return duplicatedErrors;
    }

    private TransactWriteItemsEnhancedRequest createTransactWriteItems(
            List<TransactPutItemEnhancedRequest<NotificationEntity>> notificationRequestList,
            List<TransactPutItemEnhancedRequest<NotificationCostEntity>> costRequestList,
            List<TransactPutItemEnhancedRequest<NotificationQREntity>> qrRequestList
    ) {
        TransactWriteItemsEnhancedRequest.Builder requestBuilder = TransactWriteItemsEnhancedRequest.builder();

        for ( TransactPutItemEnhancedRequest<NotificationEntity> putItemNotification: notificationRequestList ) {
            requestBuilder.addPutItem( dynamoDbTable, putItemNotification);
        }
        for ( TransactPutItemEnhancedRequest<NotificationCostEntity> putItemCost : costRequestList  ) {
            requestBuilder.addPutItem( dynamoDbCostTable, putItemCost );
        }
        for ( TransactPutItemEnhancedRequest<NotificationQREntity> putItemQR : qrRequestList ) {
            requestBuilder.addPutItem( dynamoDbQRTable, putItemQR );
        }
        return requestBuilder.build();
    }

    private List<TransactPutItemEnhancedRequest<NotificationCostEntity>> createCostPutItemRequests(List<NotificationCostEntity> notificationCostEntityList) {
        List<TransactPutItemEnhancedRequest<NotificationCostEntity>> putItemEnhancedRequestList = new ArrayList<>();
        Expression conditionExpressionPut = Expression.builder()
                .expression("attribute_not_exists(creditorTaxId_noticeCode)")
                .build();
        for ( NotificationCostEntity costEntity : notificationCostEntityList ) {
            putItemEnhancedRequestList.add( TransactPutItemEnhancedRequest.builder( NotificationCostEntity.class )
                    .item( costEntity )
                    .conditionExpression( conditionExpressionPut )
                    .build()
            );
        }
        return putItemEnhancedRequestList;
    }

    private List<TransactPutItemEnhancedRequest<NotificationQREntity>> createQRPutItemRequests(List<NotificationQREntity> notificationQREntityList) {
        List<TransactPutItemEnhancedRequest<NotificationQREntity>> putItemEnhancedRequestList = new ArrayList<>();
        Expression conditionExpressionPut = Expression.builder()
                .expression( "attribute_not_exists(aarQRCodeValue)" )
                .build();
        for ( NotificationQREntity qrEntity : notificationQREntityList ) {
            putItemEnhancedRequestList.add( TransactPutItemEnhancedRequest.builder( NotificationQREntity.class )
                    .item( qrEntity )
                    .conditionExpression( conditionExpressionPut )
                    .build()
            );
        }
        return putItemEnhancedRequestList;
    }

    private List<NotificationCostEntity> getNotificationCostEntities(NotificationEntity notificationEntity) {
        List<NotificationCostEntity> notificationCostEntityList = new ArrayList<>();

        for (NotificationRecipientEntity rec : notificationEntity.getRecipients() ) {
            for ( NotificationPaymentInfoEntity payment : rec.getPaymentList() )
                if ( Objects.nonNull( payment ) ) {
                    NotificationCostEntity notificationCostEntity = NotificationCostEntity.builder()
                            .recipientType ( rec.getRecipientType().getValue() )
                            .recipientIdx( notificationEntity.getRecipients().indexOf( rec ) )
                            .iun( notificationEntity.getIun() )
                            .creditorTaxIdNoticeCode( payment.getCreditorTaxId() + "##" + payment.getNoticeCode() )
                            .build();
                    notificationCostEntityList.add(notificationCostEntity);
                }
            }
        return notificationCostEntityList;
    }

    private List<NotificationQREntity> getNotificationQREntities(NotificationEntity notificationEntity) {
        List<NotificationQREntity> notificationQREntityList = new ArrayList<>();

        for ( NotificationRecipientEntity rec : notificationEntity.getRecipients() ) {
            notificationQREntityList.add( NotificationQREntity.builder()
                    .recipientType( rec.getRecipientType() )
                    .iun( notificationEntity.getIun() )
                    .recipientId( rec.getRecipientId() )
                    .aarQRCodeValue( generateToken( notificationEntity.getIun(), rec.getRecipientId()) )
                    .build());
        }
        return notificationQREntityList;
    }

    private String generateToken(String iun, String taxId) {
      byte[] bytes = (iun + "_" + taxId + "_" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
      return Base64Utils.encodeToUrlSafeString( bytes ).replace("=","");
    }

}

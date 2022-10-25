package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationRecipientEntity;
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
        List<PutItemEnhancedRequest<NotificationEntity>> notificationRequestList = createNotificationPutItemRequests( notificationEntity );

        List<NotificationCostEntity> notificationCostEntityList = getNotificationCostEntities( notificationEntity );

        List<NotificationQREntity> notificationQREntityList = getNotificationQREntities( notificationEntity );

        List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList = createCostPutItemRequests( notificationCostEntityList );

        List<PutItemEnhancedRequest<NotificationQREntity>> qrRequestList = createQRPutItemRequests( notificationQREntityList );

        TransactWriteItemsEnhancedRequest enhancedRequest = createTransactWriteItems( notificationRequestList, costRequestList, qrRequestList );

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            Map<String,String> duplicatedErrors = getDuplicationErrors(notificationEntity, notificationCostEntityList);
            throw new PnIdConflictException( duplicatedErrors );
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


        NotificationEntity controlIdempotenceToken = NotificationEntity.builder()
                .iun( getControlIdempotenceToken(notificationEntity) )
                .requestId( Base64Utils.encodeToString( notificationEntity.getIun().getBytes(StandardCharsets.UTF_8) ) )
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
            duplicatedErrors.put( "Duplicated notification for iun", notificationEntity.getIun() );
        }
        if ( Objects.nonNull( idempotenceTokenDuplicated ) ) {
            duplicatedErrors.put("Duplicated notification for senderPaId##paProtocolNumber##idempotenceToken" , controlIdempotenceToken );
        }
        for ( NotificationCostEntity nce : costEntitiesDuplicated ) {
            duplicatedErrors.put("Duplicated notification for creditorTaxId##noticeCode", nce.getCreditorTaxId_noticeCode());
        }
        return duplicatedErrors;
    }

    private TransactWriteItemsEnhancedRequest createTransactWriteItems(
            List<PutItemEnhancedRequest<NotificationEntity>> notificationRequestList,
            List<PutItemEnhancedRequest<NotificationCostEntity>> costRequestList,
            List<PutItemEnhancedRequest<NotificationQREntity>> qrRequestList
    ) {
        TransactWriteItemsEnhancedRequest.Builder requestBuilder = TransactWriteItemsEnhancedRequest.builder();

        for ( PutItemEnhancedRequest<NotificationEntity> putItemNotification: notificationRequestList ) {
            requestBuilder.addPutItem( dynamoDbTable, putItemNotification);
        }
        for (PutItemEnhancedRequest<NotificationCostEntity> putItemCost : costRequestList  ) {
            requestBuilder.addPutItem( dynamoDbCostTable, putItemCost );
        }
        for ( PutItemEnhancedRequest<NotificationQREntity> putItemQR : qrRequestList ) {
            requestBuilder.addPutItem( dynamoDbQRTable, putItemQR );
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

    private List<PutItemEnhancedRequest<NotificationQREntity>> createQRPutItemRequests(List<NotificationQREntity> notificationQREntityList) {
        List<PutItemEnhancedRequest<NotificationQREntity>> putItemEnhancedRequestList = new ArrayList<>();
        Expression conditionExpressionPut = Expression.builder()
                .expression( "attribute_not_exists(aarQRCodeValue)" )
                .build();
        for ( NotificationQREntity qrEntity : notificationQREntityList ) {
            putItemEnhancedRequestList.add( PutItemEnhancedRequest.builder( NotificationQREntity.class )
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
        
        if (Objects.nonNull(rec.getPayment())) {
          NotificationCostEntity notificationCostEntity = NotificationCostEntity.builder()
              .recipientIdx( notificationEntity.getRecipients().indexOf( rec ) )
              .iun( notificationEntity.getIun() )            
              .creditorTaxId_noticeCode( rec.getPayment().getCreditorTaxId() + "##" + rec.getPayment().getNoticeCode() )
              .build();

              notificationCostEntityList.add(notificationCostEntity);
      
                  if ( rec.getPayment().getNoticeCodeAlternative() != null ) {
                      notificationCostEntityList.add( NotificationCostEntity.builder()
                      .recipientIdx( notificationEntity.getRecipients().indexOf( rec ) )
                     .iun( notificationEntity.getIun() )
                     .creditorTaxId_noticeCode( rec.getPayment().getCreditorTaxId() + "##" + rec.getPayment().getNoticeCodeAlternative() )
                     .build()
                  );
              
              }
          
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
                            .aarQRCodeValue( notificationEntity.getTokens().get( rec.getRecipientId() ) )
                    .build());
        }

        return notificationQREntityList;
    }


}

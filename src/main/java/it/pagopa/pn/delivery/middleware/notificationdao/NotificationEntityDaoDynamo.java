package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NotificationEntityDaoDynamo extends AbstractDynamoKeyValueStore<NotificationEntity> implements NotificationEntityDao {
    private static final String INDEX_CREDITOR_TAX_ID_NOTICE_CODE = "creditorTaxId_noticeCode";
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private final DynamoDbTable<NotificationEntity> dynamoDbTable;
    private final EntityToDtoNotificationMapper entity2DtoMapper;

    public NotificationEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryConfigs cfg, EntityToDtoNotificationMapper entity2DtoMapper) {
        super(dynamoDbEnhancedClient.table(tableName( cfg ), TableSchema.fromClass(NotificationEntity.class)));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.entity2DtoMapper = entity2DtoMapper;
        this.dynamoDbTable = dynamoDbEnhancedClient.table(tableName( cfg), TableSchema.fromClass(NotificationEntity.class));

    }

    private static String tableName( PnDeliveryConfigs cfg ) {
        return cfg.getNotificationDao().getTableName();
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

        PutItemEnhancedRequest<NotificationEntity> request2 = PutItemEnhancedRequest.builder( NotificationEntity.class )
                .item( controlNotificationEntity )
                .conditionExpression( conditionExpressionPut )
                .build();

        TransactWriteItemsEnhancedRequest enhancedRequest = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(dynamoDbTable, request1 )
                .addPutItem(dynamoDbTable, request2 )
                .build();

        try {
            dynamoDbEnhancedClient.transactWriteItems( enhancedRequest );
        } catch (TransactionCanceledException ex) {
            log.error("Unable to insert notification={}", notificationEntity ,ex );
            throw new IdConflictException( notificationEntity );
        }
    }

    @NotNull
    private String getControlIun(NotificationEntity notificationEntity) {
        return notificationEntity.getSenderPaId()
                + "##" + notificationEntity.getPaNotificationId()
                + "##" + notificationEntity.getCancelledIun();
    }

    @Override
    public Optional<List<InternalNotification>> getNotificationByPaymentInfo(String paTaxId, String noticeNumber) {
        DynamoDbIndex<NotificationEntity> index = table.index( INDEX_CREDITOR_TAX_ID_NOTICE_CODE );

        Key key = Key.builder()
                .partitionValue( paTaxId+"##"+noticeNumber )
                .build();

        QueryConditional queryConditional = QueryConditional
                .keyEqualTo( key );

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional( queryConditional )
                .build();

        SdkIterable<Page<NotificationEntity>> notificationEntityPages = index.query( request );
        Page<NotificationEntity> page = notificationEntityPages.iterator().next();
        return Optional.of( page.items()
                .stream()
                .map( entity2DtoMapper::entity2Dto )
                .collect( Collectors.toList())
        );
    }
}

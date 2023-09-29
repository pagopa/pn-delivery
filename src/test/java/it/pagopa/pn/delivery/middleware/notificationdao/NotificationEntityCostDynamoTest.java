package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
class NotificationEntityCostDynamoTest {

    private static DynamoDbTable<NotificationCostEntity> costStorageTable;

    private static final String IUN = "UHQX-NMVP-ZKDQ-202210-H-1";
    public static final String CREDITOR_TAX_ID_NOTICE_CODE = "77777777777##002720356512737953";
    public static final String CREDITOR_TAX_ID = "77777777777";
    public static final String NOTICE_CODE = "002720356512737953";

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private PnDeliveryConfigs.NotificationCostDao notificationCostDao;

    @Mock
    private PnDeliveryConfigs cfg;

    private NotificationCostEntityDao costEntityDao;



    @BeforeEach
    void setup() {
        notificationCostDao = mock(PnDeliveryConfigs.NotificationCostDao.class);
        costStorageTable = mock(DynamoDbTable.class);
        dynamoDbEnhancedClient = mock( DynamoDbEnhancedClient.class );
        Mockito.when( cfg.getNotificationCostDao() ).thenReturn( notificationCostDao );
        Mockito.when( cfg.getNotificationCostDao().getTableName() ).thenReturn( "NotificationsCost" );
        Mockito.when( dynamoDbEnhancedClient.table( Mockito.anyString(), Mockito.any() )).then( in -> costStorageTable );
        costEntityDao = new NotificationEntityCostDynamo( dynamoDbEnhancedClient, cfg );
    }

    @Test
    void deleteWithCheckIun(){
        assertDoesNotThrow(() -> costEntityDao.deleteWithCheckIun("TAXID",NOTICE_CODE, IUN));
    }

    @Test
    void throwDeleteWithCheckIun(){
        Mockito.when( costStorageTable.deleteItem((DeleteItemEnhancedRequest) Mockito.any()) ).thenThrow(ConditionalCheckFailedException.class);
        assertDoesNotThrow(() -> costEntityDao.deleteWithCheckIun("TAXID",NOTICE_CODE, IUN));
    }

    @Test
    void getNotificationByPaymentInfoSuccess() {

        NotificationCostEntity entityToInsert = NotificationCostEntity.builder()
                .iun( IUN )
                .recipientIdx( 0 )
                .creditorTaxIdNoticeCode( CREDITOR_TAX_ID_NOTICE_CODE )
                .build();

        costEntityDao.putIfAbsent( entityToInsert );

        Mockito.when( costStorageTable.getItem( Mockito.any(Key.class) ) ).thenReturn( entityToInsert );

        Optional<InternalNotificationCost> result = costEntityDao.getNotificationByPaymentInfo( CREDITOR_TAX_ID, NOTICE_CODE );

        Assertions.assertTrue( result.isPresent() );
        Assertions.assertNotNull( result.get() );
    }

    @Test
    void deleteItem() {

        NotificationCostEntity entityToInsert = NotificationCostEntity.builder()
                .iun( IUN )
                .recipientIdx( 0 )
                .creditorTaxIdNoticeCode( CREDITOR_TAX_ID_NOTICE_CODE )
                .build();

        costEntityDao.putIfAbsent( entityToInsert );

        Mockito.when( costStorageTable.getItem( Mockito.any(Key.class) ) ).thenReturn( entityToInsert );

        Optional<InternalNotificationCost> resultInsert = costEntityDao.getNotificationByPaymentInfo( CREDITOR_TAX_ID, NOTICE_CODE );

        Assertions.assertTrue( resultInsert.isPresent() );
        Assertions.assertNotNull( resultInsert.get() );

        Mockito.when( costStorageTable.getItem( Mockito.any(Key.class) ) ).thenReturn( null );

        costEntityDao.deleteItem(entityToInsert);

        Optional<InternalNotificationCost> result = costEntityDao.getNotificationByPaymentInfo( CREDITOR_TAX_ID, NOTICE_CODE );

        Assertions.assertTrue( result.isEmpty() );
    }

}

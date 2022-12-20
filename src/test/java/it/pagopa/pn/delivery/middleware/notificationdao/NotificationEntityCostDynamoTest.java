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

import java.util.Optional;


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
        notificationCostDao = Mockito.mock(PnDeliveryConfigs.NotificationCostDao.class);
        costStorageTable = Mockito.mock(DynamoDbTable.class);
        dynamoDbEnhancedClient = Mockito.mock( DynamoDbEnhancedClient.class );
        Mockito.when( cfg.getNotificationCostDao() ).thenReturn( notificationCostDao );
        Mockito.when( cfg.getNotificationCostDao().getTableName() ).thenReturn( "NotificationsCost" );
        Mockito.when( dynamoDbEnhancedClient.table( Mockito.anyString(), Mockito.any() )).then( in -> costStorageTable );
        costEntityDao = new NotificationEntityCostDynamo( dynamoDbEnhancedClient, cfg );
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

}

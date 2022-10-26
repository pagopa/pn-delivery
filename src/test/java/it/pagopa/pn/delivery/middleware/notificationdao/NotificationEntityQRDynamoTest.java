package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.RecipientTypeEntity;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
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
class NotificationEntityQRDynamoTest {

    private static DynamoDbTable<NotificationQREntity> qrStorageTable;

    private static final String IUN = "UHQX-NMVP-ZKDQ-202210-H-1";
    private static final String RECIPIENT_ID = "PF-aa0c4556-5a6f-45b1-800c-0f4f3c5a57b6";
    private static final String AAR_QR_CODE_VALUE = "VUhRWC1OTVZQLVpLRFEtMjAyMjEwLUgtMV9GUk1UVFI3Nk0wNkI3MTVFXzIyYzJlNDc0LTFmMzgtNGY4Zi04M2FjLWUxOWVlYTFkZTczNg";

    @Mock
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Mock
    private PnDeliveryConfigs.NotificationQRDao notificationQRDao;

    @Mock
    private PnDeliveryConfigs cfg;

    private NotificationQREntityDao qrEntityDao;

    @BeforeEach
    void setup() {
        notificationQRDao = Mockito.mock(PnDeliveryConfigs.NotificationQRDao.class);
        qrStorageTable = Mockito.mock(DynamoDbTable.class);
        dynamoDbEnhancedClient = Mockito.mock( DynamoDbEnhancedClient.class );
        Mockito.when( cfg.getNotificationQRDao() ).thenReturn( notificationQRDao );
        Mockito.when( cfg.getNotificationQRDao().getTableName() ).thenReturn( "NotificationsQR" );
        Mockito.when( dynamoDbEnhancedClient.table( Mockito.anyString(), Mockito.any() )).then( in -> qrStorageTable );
        qrEntityDao = new NotificationEntityQRDynamo( dynamoDbEnhancedClient, cfg );
    }

    @Test
    void getNotificationByQRSuccess() {

        NotificationQREntity entityToInsert = NotificationQREntity.builder()
                .iun( IUN )
                .recipientId( RECIPIENT_ID )
                .recipientType( RecipientTypeEntity.PF )
                .aarQRCodeValue( AAR_QR_CODE_VALUE )
                .build();

        qrEntityDao.putIfAbsent( entityToInsert );

        Mockito.when( qrStorageTable.getItem( Mockito.any( Key.class ) ) ).thenReturn( entityToInsert );

        Optional<InternalNotificationQR> result = qrEntityDao.getNotificationByQR( AAR_QR_CODE_VALUE );

        Assertions.assertTrue( result.isPresent() );
        Assertions.assertNotNull( result.get() );

    }

}

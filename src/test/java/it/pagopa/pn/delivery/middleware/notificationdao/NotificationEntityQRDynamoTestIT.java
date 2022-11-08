package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.RecipientTypeEntity;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        NotificationEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
        "pn.delivery.notification-dao.table-name=Notifications",
        "pn.delivery.notification-cost-dao.table-name=NotificationsCost",
        "pn.delivery.notification-metadata-dao.table-name=NotificationsMetadata",
        "pn.delivery.notification-qr-dao.table-name=NotificationsQR"
})
@SpringBootTest
class NotificationEntityQRDynamoTestIT {

    private static final String IUN = "UHQX-NMVP-ZKDQ-202210-H-1";
    private static final String RECIPIENT_ID = "PF-aa0c4556-5a6f-45b1-800c-0f4f3c5a57b6";
    private static final String AAR_QR_CODE_VALUE = "VUhRWC1OTVZQLVpLRFEtMjAyMjEwLUgtMV9GUk1UVFI3Nk0wNkI3MTVFXzIyYzJlNDc0LTFmMzgtNGY4Zi04M2FjLWUxOWVlYTFkZTczNg";

    @Autowired
    private NotificationEntityDao notificationEntityDao;

    @Autowired
    private NotificationDaoDynamo notificationDao;

    @Autowired
    private NotificationCostEntityDao notificationCostEntityDao;

    @Autowired
    private NotificationQREntityDao notificationQREntityDao;


    @Test
    void getNotificationByQR() {

        NotificationQREntity entity = NotificationQREntity.builder()
                .aarQRCodeValue( AAR_QR_CODE_VALUE )
                .iun( IUN )
                .recipientType( RecipientTypeEntity.PF )
                .recipientId( RECIPIENT_ID )
                .build();

        notificationQREntityDao.putIfAbsent( entity );

        Optional<InternalNotificationQR> elementFromDb = notificationQREntityDao.getNotificationByQR( AAR_QR_CODE_VALUE );

        assertTrue( elementFromDb.isPresent() );
        assertEquals( AAR_QR_CODE_VALUE, elementFromDb.get().getAarQRCodeValue() );
        assertEquals( IUN, elementFromDb.get().getIun() );
        assertEquals( RECIPIENT_ID, elementFromDb.get().getRecipientInternalId() );
    }
    
    
    @Test
    void getNotificationQR() {

        NotificationQREntity entity = NotificationQREntity.builder()
                .aarQRCodeValue( AAR_QR_CODE_VALUE )
                .iun( IUN )
                .recipientType( RecipientTypeEntity.PF )
                .recipientId( RECIPIENT_ID )
                .build();

        notificationQREntityDao.putIfAbsent( entity );

        Map<String, String> elementFromDb = notificationQREntityDao.getQR( IUN );

        assertTrue( !elementFromDb.isEmpty() );
        assertTrue( elementFromDb.containsKey(RECIPIENT_ID) );
        assertEquals( AAR_QR_CODE_VALUE, elementFromDb.get(RECIPIENT_ID) );
    }
}

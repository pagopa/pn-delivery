package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.RecipientTypeEntity;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityQRDynamoTest {

    private static final String IUN = "UHQX-NMVP-ZKDQ-202210-H-1";
    private static final String RECIPIENT_ID = "PF-aa0c4556-5a6f-45b1-800c-0f4f3c5a57b6";
    private static final String AAR_QR_CODE_VALUE = "VUhRWC1OTVZQLVpLRFEtMjAyMjEwLUgtMV9GUk1UVFI3Nk0wNkI3MTVFXzIyYzJlNDc0LTFmMzgtNGY4Zi04M2FjLWUxOWVlYTFkZTczNg";
    private NotificationQREntityDao qrEntityDao;

    @BeforeEach
    void setup() { qrEntityDao = new NotificationQREntityDaoMock(); }

    @Test
    void getNotificationByQRSuccess() {

        NotificationQREntity entityToInsert = NotificationQREntity.builder()
                .iun( IUN )
                .recipientId( RECIPIENT_ID )
                .recipientType( RecipientTypeEntity.PF )
                .aarQRCodeValue( AAR_QR_CODE_VALUE )
                .build();

        qrEntityDao.putIfAbsent( entityToInsert );

        Optional<InternalNotificationQR> result = qrEntityDao.getNotificationByQR( AAR_QR_CODE_VALUE );

        Assertions.assertTrue( result.isPresent() );
        Assertions.assertNotNull( result.get() );

    }

    private static class NotificationQREntityDaoMock implements NotificationQREntityDao {

        private final Map<Key, NotificationQREntity> storage = new ConcurrentHashMap<>();

        @Override
        public Optional<InternalNotificationQR> getNotificationByQR(String aarQRCode) {
            Key key = Key.builder()
                    .partitionValue( aarQRCode )
                    .build();

            NotificationQREntity notificationQREntity = storage.get( key );
            if ( Objects.nonNull(notificationQREntity) ) {
                return Optional.of( InternalNotificationQR.builder()
                        .iun( notificationQREntity.getIun() )
                        .aarQRCodeValue( notificationQREntity.getAarQRCodeValue() )
                        .recipientInternalId( notificationQREntity.getRecipientId() )
                        .recipientType( NotificationRecipient.RecipientTypeEnum.valueOf( notificationQREntity.getRecipientType().getValue() ))
                        .build() );
            } else {
                return Optional.empty();
            }
        }

        @Override
        public void put(NotificationQREntity notificationQREntity) {
            Key key = Key.builder()
                    .partitionValue( notificationQREntity.getAarQRCodeValue() )
                    .build();
            storage.put( key, notificationQREntity );
        }

        @Override
        public void putIfAbsent(NotificationQREntity notificationQREntity) throws PnIdConflictException {
            Key key = Key.builder()
                    .partitionValue( notificationQREntity.getAarQRCodeValue() )
                    .build();
            storage.putIfAbsent( key, notificationQREntity );
        }

        @Override
        public Optional<NotificationQREntity> get(Key key) {
            return Optional.empty();
        }

        @Override
        public void delete(Key key) {

        }
    }

}
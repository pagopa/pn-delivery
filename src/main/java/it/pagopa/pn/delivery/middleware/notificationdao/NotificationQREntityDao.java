package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationQREntity;
import it.pagopa.pn.delivery.models.InternalNotificationQR;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

public interface NotificationQREntityDao extends KeyValueStore<Key, NotificationQREntity> {

    Optional<InternalNotificationQR> getNotificationByQR( String aarQRCode );
}

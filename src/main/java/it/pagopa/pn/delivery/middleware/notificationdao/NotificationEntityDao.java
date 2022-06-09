package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface NotificationEntityDao extends KeyValueStore<Key, NotificationEntity> {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";
}

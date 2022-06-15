package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationCostEntity;
import it.pagopa.pn.delivery.models.NotificationCost;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

public interface NotificationCostEntityDao extends KeyValueStore<Key, NotificationCostEntity> {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";
    Optional<NotificationCost> getNotificationByPaymentInfo(String paTaxId, String noticeNumber );
}

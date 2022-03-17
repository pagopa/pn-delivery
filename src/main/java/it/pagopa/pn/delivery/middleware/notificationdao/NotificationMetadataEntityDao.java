package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.commons.abstractions.KeyValueStore;

import java.util.List;

public interface NotificationMetadataEntityDao<K,E> extends KeyValueStore<K,E> {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    List<NotificationSearchRow> searchNotificationMetadata(InputSearchNotificationDto inputSearchNotificationDto);
}

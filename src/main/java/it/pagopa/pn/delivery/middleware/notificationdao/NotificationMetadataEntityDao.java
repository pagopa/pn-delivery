package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

public interface NotificationMetadataEntityDao<K,E> extends KeyValueStore<K,E> {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchNotificationMetadata(InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey);
}

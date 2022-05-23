package it.pagopa.pn.delivery.middleware;



import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

import java.util.Optional;

public interface NotificationDao {

    static final String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    void addNotification(InternalNotification notification) throws IdConflictException;

    Optional<InternalNotification> getNotificationByIun(String iun);

    ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );
}



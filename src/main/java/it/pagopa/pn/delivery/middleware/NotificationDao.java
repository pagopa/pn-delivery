package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

import java.util.Optional;

public interface NotificationDao {

    static final String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    void addNotification(Notification notification) throws IdConflictException;

    Optional<Notification> getNotificationByIun(String iun);

    ResultPaginationDto<NotificationSearchRow,PnLastEvaluatedKey> searchNotification(InputSearchNotificationDto inputSearchNotificationDto, PnLastEvaluatedKey lastEvaluatedKey);
}



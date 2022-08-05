package it.pagopa.pn.delivery.middleware;


import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

import java.util.Optional;

public interface NotificationDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    default void addNotification(InternalNotification notification) throws IdConflictException {
        this.addNotification( notification, null );
    }

    void addNotification(InternalNotification notification, Runnable doBeforeSave) throws IdConflictException;

    Optional<InternalNotification> getNotificationByIun(String iun);

    Optional<String> getRequestId( String senderId, String paProtocolNumber, String idempotenceToken );

    PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );

    PageSearchTrunk<NotificationMetadataEntity> searchByIUN(
            InputSearchNotificationDto inputSearchNotificationDto
    );

}



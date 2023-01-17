package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;

import java.util.Optional;

public interface NotificationDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification-dao";

    void addNotification(InternalNotification notification) throws PnIdConflictException;

    Optional<InternalNotification> getNotificationByIun(String iun);

    Optional<String> getRequestId( String senderId, String paProtocolNumber, String idempotenceToken );

    PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(
            InputSearchNotificationDto inputSearchNotificationDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );

    PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedForOneMonth(
            InputSearchNotificationDelegatedDto searchDto,
            String indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );

    PageSearchTrunk<NotificationMetadataEntity> searchByIUN(
            InputSearchNotificationDto inputSearchNotificationDto
    );

}

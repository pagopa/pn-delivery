package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.Optional;

public interface NotificationDao {

    void addNotification(InternalNotification notification) throws PnIdConflictException;

    Optional<InternalNotification> getNotificationByIun(String iun, boolean deanonymizeRecipients);

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
            IndexNameAndPartitions.SearchIndexEnum indexName,
            String partitionValue,
            int size,
            PnLastEvaluatedKey lastEvaluatedKey
    );

    PageSearchTrunk<NotificationMetadataEntity> searchByIUN(
            InputSearchNotificationDto inputSearchNotificationDto
    );

    Page<NotificationDelegationMetadataEntity> searchByPk(InputSearchNotificationDelegatedDto searchDto);
}

package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV28;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.PageSearchTrunk;
import it.pagopa.pn.delivery.svc.search.IndexNameAndPartitions;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationDaoMock implements NotificationDao {

    private final EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

    private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();


    @Override
    public void addNotification(InternalNotification notification) throws PnIdConflictException {

    }

    @Override
    public Optional<InternalNotification> getNotificationByIun(String iun, boolean deanonymizeRecipients) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getRequestId(String senderId, String paProtocolNumber, String idempotenceToken) {
        return Optional.empty();
    }

    @Override
    public PageSearchTrunk<NotificationMetadataEntity> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {

        PageSearchTrunk<NotificationMetadataEntity> result = new PageSearchTrunk<>();
        result.setResults(Collections.singletonList( NotificationMetadataEntity.builder()
                .iunRecipientId("IUN##internalId1" )
                .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                .senderId( "SenderId" )
                .recipientIds(List.of( "internalId1", "internalId2" ) )
                .build() ));

        return result;
    }

    @Override
    public PageSearchTrunk<NotificationDelegationMetadataEntity> searchDelegatedForOneMonth(InputSearchNotificationDelegatedDto searchDto, IndexNameAndPartitions.SearchIndexEnum indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {
        PageSearchTrunk<NotificationDelegationMetadataEntity> result = new PageSearchTrunk<>();
        result.setResults(Collections.singletonList(NotificationDelegationMetadataEntity.builder()
                .iunRecipientIdDelegateIdGroupId("IUN##recipientId##delegateId")
                .notificationStatus(NotificationStatusV28.VIEWED.getValue())
                        .senderId("senderId")
                        .recipientId("recipientId")
                        .recipientIds(List.of("recipientId"))
                .build()));
        return result;
    }

    @Override
    public PageSearchTrunk<NotificationMetadataEntity> searchByIUN(InputSearchNotificationDto inputSearchNotificationDto) {
        PageSearchTrunk<NotificationMetadataEntity> result = new PageSearchTrunk<>();
        result.setResults(Collections.singletonList( NotificationMetadataEntity.builder()
                .iunRecipientId("IUN##internalId1" )
                .notificationStatus( NotificationStatusV28.VIEWED.getValue() )
                .senderId( "SenderId" )
                .recipientIds(List.of( "internalId1", "internalId2" ) )
                .build() ));

        return result;
    }

    @Override
    public Page<NotificationDelegationMetadataEntity> searchByPk(InputSearchNotificationDelegatedDto searchDto) {
        NotificationDelegationMetadataEntity entity = new NotificationDelegationMetadataEntity();
        entity.setIunRecipientIdDelegateIdGroupId("IUN##recipientId##delegateId");
        return Page.create(Collections.singletonList(entity), null);
    }
}

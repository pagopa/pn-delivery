package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.search.PnLastEvaluatedKey;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationDaoMock implements NotificationDao {

    private final EntityToDtoNotificationMetadataMapper entityToDto = new EntityToDtoNotificationMetadataMapper();

    private final Map<Key, NotificationMetadataEntity> storage = new ConcurrentHashMap<>();


    @Override
    public void addNotification(InternalNotification notification, Runnable runnable) throws IdConflictException {
        if ( runnable != null ) {
            runnable.run();
        }
    }

    @Override
    public Optional<InternalNotification> getNotificationByIun(String iun) {
        return Optional.empty();
    }

    @Override
    public ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchForOneMonth(InputSearchNotificationDto inputSearchNotificationDto, String indexName, String partitionValue, int size, PnLastEvaluatedKey lastEvaluatedKey) {

        return ResultPaginationDto.<NotificationSearchRow, PnLastEvaluatedKey>builder()
                .resultsPage(Collections.singletonList( NotificationSearchRow.builder()
                        .iun( "IUN" )
                        //.group( "GRP" )
                        .paProtocolNumber( "paProtocolNumber" )
                        .notificationStatus( NotificationStatus.VIEWED )
                        .sender( "SenderId" )
                        .subject( "Subject" )
                        .recipients( List.of( "internalId1", "internalId2" ) )
                        .build() ))
                .moreResult( false )
                .build();
    }
}

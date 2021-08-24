package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.CASSANDRA )
@Slf4j
public class CassandraNotificationDao implements NotificationDao {

    private final KeyValueStore<String, NotificationEntity> notificationEntityDao;
    private final DtoToEntityMapper dto2entityMapper;
    private final EntityToDtoMapper entity2dtoMapper;

    public CassandraNotificationDao( KeyValueStore<String, NotificationEntity> notificationEntityDao, DtoToEntityMapper dto2entityMapper, EntityToDtoMapper entity2dtoMapper) {
        this.notificationEntityDao = notificationEntityDao;
        this.dto2entityMapper = dto2entityMapper;
        this.entity2dtoMapper = entity2dtoMapper;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {
        NotificationEntity entity = dto2entityMapper.dto2Entity( notification );
        notificationEntityDao.putIfAbsent( entity );
    }

    @Override
    public Optional<Notification> getNotificationByIun(String iun) {
        return notificationEntityDao.get( iun )
                .map( entity2dtoMapper::entity2Dto );
    }

}

package it.pagopa.pn.delivery.middleware.cassandra;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.model.notification.cassandra.NotificationEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.CASSANDRA )
public class CassandraNotificationDao implements NotificationDao {

    private final CassandraNotificationEntityDao notificationEntityDao;
    private final DtoToEntityMapper mapper;

    public CassandraNotificationDao(CassandraNotificationEntityDao notificationEntityDao, DtoToEntityMapper mapper) {
        this.notificationEntityDao = notificationEntityDao;
        this.mapper = mapper;
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {

        NotificationEntity entity = mapper.dto2Entity( notification );
        notificationEntityDao.putIfAbsent( entity );
    }
}

package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractCassandraKeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.NotificationBySenderEntity;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.NotificationBySenderEntityId;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

@Component
@Deprecated
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.CASSANDRA )
public class CassandraNotificationBySenderEntityDao extends AbstractCassandraKeyValueStore<NotificationBySenderEntityId, NotificationBySenderEntity> {

    public CassandraNotificationBySenderEntityDao(CassandraOperations cassandraTemplate) {
        super(cassandraTemplate, NotificationBySenderEntity.class);
    }
}

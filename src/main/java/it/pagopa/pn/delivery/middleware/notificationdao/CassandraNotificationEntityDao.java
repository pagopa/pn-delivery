package it.pagopa.pn.delivery.middleware.notificationdao;


import it.pagopa.pn.commons.abstractions.impl.AbstractCassandraKeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons_delivery.model.notification.cassandra.NotificationEntity;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.CASSANDRA )
@Deprecated
public class CassandraNotificationEntityDao extends AbstractCassandraKeyValueStore<String, NotificationEntity> {

    public CassandraNotificationEntityDao(CassandraOperations cassandraTemplate) {
        super(cassandraTemplate, NotificationEntity.class);
    }
}

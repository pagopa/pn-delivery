package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty( name = NotificationDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.FAKE )
//@ConditionalOnMissingBean(CassandraOperations.class)
public class FakeNotificationDao implements NotificationDao{


    public FakeNotificationDao() {
        System.out.println("FAKE CASSANDRA GOOO!!!!!!!");
    }

    @Override
    public void addNotification(Notification notification) throws IdConflictException {

    }
}

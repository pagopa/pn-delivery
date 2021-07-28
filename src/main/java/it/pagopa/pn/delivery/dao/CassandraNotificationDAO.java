package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.delivery.model.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@ConditionalOnProperty( name="pn.key-value-store", havingValue = "cassandra")
@Component
public class CassandraNotificationDAO implements DeliveryDAO {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final CassandraNotificationRepository springDataCassandra;

    public CassandraNotificationDAO(CassandraNotificationRepository springDataCassandra) {
        this.springDataCassandra = springDataCassandra;
    }

    public CompletableFuture<Void> addNotification(Notification notification ) {
        springDataCassandra.save( notification );
        return CompletableFuture.runAsync( () -> {});
    }
}

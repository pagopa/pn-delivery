package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.delivery.model.notification.Notification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@ConditionalOnProperty( name="pn.key-value-store", havingValue = "cassandra")
@Repository
public interface CassandraNotificationRepository extends CrudRepository<Notification, String> {

}

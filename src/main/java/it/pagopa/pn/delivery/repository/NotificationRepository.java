package it.pagopa.pn.delivery.repository;

import it.pagopa.pn.delivery.model.notification.Notification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends CrudRepository<Notification, String> {


}

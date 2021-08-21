package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;

import java.util.Optional;

public interface NotificationDao {

    static final String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.delivery-dao";

    // FIXME: manca gestione annullamento
    void addNotification(Notification notification ) throws IdConflictException;

    Optional<Notification> getNotificationByIun(String iun );

    void deleteNotificationByIun( String iun );

}

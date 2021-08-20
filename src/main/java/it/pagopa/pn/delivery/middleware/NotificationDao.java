package it.pagopa.pn.delivery.middleware;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;

public interface NotificationDao {

    public static final String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.deliverydao";
    /**
     * Gestisce anche l' "annullamento" delle notifiche precedenti
     *
     * @param notification
     * @return
     */
    void addNotification(Notification notification ) throws IdConflictException;

    Notification getNotificationByIun( String iun );

    void deleteNotificationByIun( String iun );

}

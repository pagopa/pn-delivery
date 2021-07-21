package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.delivery.model.notification.Notification;

import java.util.concurrent.CompletableFuture;

public interface DeliveryDAO {

    /**
     * Gestisce anche l' "annullamento" delle notifiche precedenti
     *
     * @param notification
     * @return
     */
    CompletableFuture<Void> addNotification(Notification notification );
}

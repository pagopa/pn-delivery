package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.delivery.model.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class FakeDeliveryDAO implements DeliveryDAO {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Gestisce anche l' "annullamento" delle notifiche precedenti
     *
     * @param notification
     * @return
     */
    public CompletableFuture<Void> addNotification(Notification notification ) {
        return CompletableFuture.runAsync(() ->
            log.info("Adding notification to store. IUN = {}", notification.getIun())
        );
    }
}

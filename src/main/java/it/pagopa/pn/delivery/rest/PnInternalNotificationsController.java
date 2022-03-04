package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import org.springframework.web.bind.annotation.*;

@RestController
public class PnInternalNotificationsController {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;

    public PnInternalNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
    }

    @GetMapping("delivery-private/notifications/{iun}")
    public Notification getSentNotification( @PathVariable( name = "iun") String iun ) {
        return retrieveSvc.getNotificationInformation( iun, false );
    }
}

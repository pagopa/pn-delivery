package it.pagopa.pn.delivery.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetSentNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.sentnotification.NotificationSentService;

@RestController
public class PnSentNotificationController implements PnDeliveryRestApi_methodGetSentNotification {

    private final NotificationSentService svc;

    public PnSentNotificationController(NotificationSentService svc) {
        this.svc = svc;
    }
	
    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_PATH )
    @JsonView(value = NotificationJsonViews.Sent.class )
    public Notification getSentNotification(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @PathVariable( name = "iun") String iun
    ) {
		return svc.getSentNotification( iun );
	}
    		
}

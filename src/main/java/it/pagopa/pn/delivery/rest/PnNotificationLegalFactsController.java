package it.pagopa.pn.delivery.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetSentNotificationLegalFacts;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.receivenotification.AttachmentService;

@RestController
public class PnNotificationLegalFactsController implements PnDeliveryRestApi_methodGetSentNotificationLegalFacts {

    private final AttachmentService svc;

    public PnNotificationLegalFactsController(AttachmentService svc) {
        this.svc = svc;
    }
	
    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_SENT_LEGALFACTS_PATH )
    public Notification getSentNotificationLegalFacts( 
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER ) String userId,
            @PathVariable( name = "iun") String iun
    ) {
		return svc.getNotificationLegalFacts( iun );
	}
    		
}

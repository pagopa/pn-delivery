package it.pagopa.pn.delivery.rest;

import javax.validation.constraints.NotBlank;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodNotificationAcknowledgement;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.notificationacknoledgement.NotificationAcknoledgementService;

@RestController
public class PnNotificationAcknowledgementDeliveryController implements PnDeliveryRestApi_methodNotificationAcknowledgement {

    private final NotificationAcknoledgementService svc;

    public PnNotificationAcknowledgementDeliveryController(NotificationAcknoledgementService svc) {
        this.svc = svc;
    }

	@Override
    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_ACKNOWLEDGEMENT_PATH + "/{iun}/{documentIndex}" )
	@ResponseBody
    public ResponseEntity<Resource> notificationAcknowledgement(
    		@NotBlank @PathVariable("iun") String iun,
    		@NotBlank @PathVariable("documentIndex") int documentIndex
    ) {
		
    	return svc.notificationAcknowledgement( iun, documentIndex );
    }

}

package it.pagopa.pn.delivery.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.dto.LegalFactsRow;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetSentNotificationLegalFacts;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.receivenotification.AttachmentService;

@RestController
public class PnSentNotificationLegalFactsController implements PnDeliveryRestApi_methodGetSentNotificationLegalFacts {

    private final AttachmentService svc;

    public PnSentNotificationLegalFactsController(AttachmentService svc) {
        this.svc = svc;
    }
	
    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_SENT_LEGALFACTS_PATH )
    public List<LegalFactsRow> getSentNotificationLegalFacts( 
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @PathVariable( name = "iun") String iun
    ) {
		return svc.sentNotificationLegalFacts( iun );
	}
    		
}

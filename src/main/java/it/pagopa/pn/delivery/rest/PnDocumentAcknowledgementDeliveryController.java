package it.pagopa.pn.delivery.rest;

import javax.validation.constraints.NotBlank;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodDocumentAcknowledgement;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.documentacknoledgement.DocumentAcknoledgementService;

@RestController
public class PnDocumentAcknowledgementDeliveryController implements PnDeliveryRestApi_methodDocumentAcknowledgement {

    private final DocumentAcknoledgementService svc;

    public PnDocumentAcknowledgementDeliveryController(DocumentAcknoledgementService svc) {
        this.svc = svc;
    }

	@Override
    @GetMapping( PnDeliveryRestConstants.SENDER_DOCUMENTACKNOWLEDGEMENT_PATH + "/{iun}/{documentIndex}" )
	@ResponseBody
    public ResponseEntity<Resource> documentAcknowledgement(
    		@NotBlank @PathVariable("iun") String iun,
    		@NotBlank @PathVariable("documentIndex") int documentIndex
    ) {
		
    	return svc.downloadDocument( iun, documentIndex );
    }

}

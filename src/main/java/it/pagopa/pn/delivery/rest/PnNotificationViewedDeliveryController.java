package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetReceivedNotificationDocuments;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.svc.notificationviewed.NotificationViewedService;

import javax.validation.constraints.NotBlank;

@RestController
public class PnNotificationViewedDeliveryController implements PnDeliveryRestApi_methodGetReceivedNotificationDocuments {

    private final NotificationViewedService svc;

    public PnNotificationViewedDeliveryController(NotificationViewedService svc) {
        this.svc = svc;
    }

	@Override
    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_VIEWED_PATH )
	public ResponseEntity<Resource> getReceivedNotificationDocument(
			@NotBlank @PathVariable("iun") String iun,
			@PathVariable("documentIndex") int documentIndex,
			@NotBlank @RequestHeader(name = "X-PagoPA-User-Id" ) String userId)
	{
		ResponseEntity<Resource> resource = svc.notificationViewed( iun, documentIndex, userId );
			
		String extension;
		if ( resource.getHeaders().getContentType() != null ) {
			extension = resource.getHeaders().getContentType().getSubtype();
		} else {
			throw new PnInternalException( "Error while retrieving the content type of the file to download" );
		}
				
		String fileName = iun + "_doc" + documentIndex + "." + extension; 	//FIXME gestire meglio estensione in base al content-type
		
		HttpHeaders headers = new HttpHeaders();
		headers.addAll( resource.getHeaders() );
		headers.add( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName );
	
		return ResponseEntity.status(resource.getStatusCode()).headers( headers ).body( resource.getBody() );
    }


}

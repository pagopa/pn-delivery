package it.pagopa.pn.delivery.rest;

import javax.validation.constraints.NotBlank;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodNotificationViewed;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.svc.notificationviewed.NotificationViewedService;

@RestController
public class PnNotificationViewedDeliveryController implements PnDeliveryRestApi_methodNotificationViewed {

    private final NotificationViewedService svc;

    public PnNotificationViewedDeliveryController(NotificationViewedService svc) {
        this.svc = svc;
    }

	@Override
    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_VIEWED_PATH )
	@ResponseBody
    public ResponseEntity<Resource> notificationViewed(
    		@NotBlank @PathVariable("iun") String iun,
    		@NotBlank @PathVariable("documentIndex") int documentIndex,
    		@RequestHeader(name =  PnDeliveryRestConstants.USER_ID_HEADER, required = false) String userId1, 	// FIXME GA RENDERE OBBLIGATORIO
    		@RequestParam(name = PnDeliveryRestConstants.USER_ID_HEADER, required = false ) String userId2	 	// FIXME GA RIMUOVERE
    ) {
		// FIXME GA RIMUOVERE QUESTA PARTE DI CODICE, l'header userId sara' valorizzato dal layer di autenticazione
		String userId;
		if ( StringUtils.isBlank( userId1 ) ) {
			userId = userId2;
		} else {
			userId = userId1;
		}
		
		if ( StringUtils.isBlank( userId ) ) {
			throw new IllegalArgumentException( PnDeliveryRestConstants.USER_ID_HEADER + " header or querystring are required" );
		}
		// FIXME GA FINE CODICE DA RIMUOVERE

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

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public class AttachmentRestUtils {

    public static final ResponseEntity<Resource> prepareAttachment( ResponseEntity<Resource> resource, String iun, String suffix ) {
        String extension;
        if ( resource.getHeaders().getContentType() != null ) {
            extension = resource.getHeaders().getContentType().getSubtype();
        } else {
            throw new PnInternalException( "Error while retrieving the content type of the file to download" );
        }

        String fileName = iun + "_" + suffix + "." + extension; 	//FIXME gestire meglio estensione in base al content-type

        HttpHeaders headers = new HttpHeaders();
        headers.addAll( resource.getHeaders() );
        headers.add( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName );

        return ResponseEntity.status(resource.getStatusCode()).headers( headers ).body( resource.getBody() );
    }

}

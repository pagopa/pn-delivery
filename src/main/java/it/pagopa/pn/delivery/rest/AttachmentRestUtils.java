package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class AttachmentRestUtils {

    private AttachmentRestUtils() {}

    public static final ResponseEntity<Resource> prepareAttachment( ResponseEntity<Resource> resource, String iun, String suffix ) {
        String extension = getExtension( resource );
        String fileName = iun + "_" + suffix + "." + extension; 	//FIXME gestire meglio estensione in base al content-type

        HttpHeaders headers = new HttpHeaders();
        headers.addAll( resource.getHeaders() );
        headers.add( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName );

        return ResponseEntity.status(resource.getStatusCode()).headers( headers ).body( resource.getBody() );
    }

    @NotNull
    private static String getExtension(ResponseEntity<Resource> resource) {
        String extension;

        HttpHeaders headers = resource.getHeaders();
        MediaType contentType = headers.getContentType();
        if ( contentType != null ) {
            extension = contentType.getSubtype();
        } else {
            throw new PnInternalException( "Error while retrieving the content type of the file to download" );
        }

        return extension;
    }

}

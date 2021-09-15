package it.pagopa.pn.delivery.svc.notificationacknoledgement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@Slf4j
public class NotificationAcknoledgementService {

	private final FileStorage fileStorage;

	@Autowired
	public NotificationAcknoledgementService( FileStorage fileStorage ) {
		this.fileStorage = fileStorage;
	}

	/**
	 * Download a document for the S3 storage
	 *
	 * @param iun unique identifier of a Notification
	 * @param documentIndex index of the documents array
	 * 
	 * @return HTTP response containingthe status code, the headers, and the body
 	 *	of the document to download
	 * 
	 */
	public ResponseEntity<Resource> notificationAcknowledgement(String iun, int documentIndex) {
		log.debug( "Document download START for iun and documentIndex {}" , iun, documentIndex);
		 
		String keyPrefix = iun + "/legalfacts/";
		
		List<S3Object> s3ObjectList = fileStorage.getFilesByKeyPrefix( keyPrefix );
				
		if ( documentIndex >= s3ObjectList.size() ) {
			log.warn( "Document ndex out of bound for iun: " + iun + " and documentIndex: " + documentIndex );
			throw new PnInternalException( "Document ndex out of bound for iun: " + iun + " and documentIndex: " + documentIndex );
		}
		
		String documentKey = s3ObjectList.get( documentIndex ).key();
		ResponseInputStream<GetObjectResponse> s3Object = fileStorage.getFileByKey( documentKey );
		
        HttpHeaders headers = headers( documentKey.substring( documentKey.lastIndexOf("/") + 1) );
	    InputStreamResource resource = null;
	    Long contentLength = null;
		try {
			byte[] bytes = s3Object.readAllBytes();
			resource = new InputStreamResource( new ByteArrayInputStream( bytes ) );
			contentLength = s3Object.response().contentLength();
		} catch ( IOException e ) {
			log.warn( "Error while retrieving document to download for iun: " + iun + " and documentIndex: " + documentIndex );
			throw new PnInternalException( "Error while retrieving document to download for iun: " + iun + " and documentIndex: " + documentIndex, e );
		}

		ResponseEntity<Resource> response = ResponseEntity.ok()
	            .headers( headers )
	            .contentLength( contentLength )
	            .contentType( MediaType.APPLICATION_OCTET_STREAM )
	            .body( resource );
		
		log.debug("downloadDocument: response {}", response);
	    return response;
	}

	private HttpHeaders headers( String fileName ) {
		HttpHeaders headers = new HttpHeaders();
        headers.add( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName );
        headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
        headers.add( "Pragma", "no-cache" );
        headers.add( "Expires", "0" );
		return headers;
	}

}

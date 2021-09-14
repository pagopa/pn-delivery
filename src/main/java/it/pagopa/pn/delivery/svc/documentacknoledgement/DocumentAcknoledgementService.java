package it.pagopa.pn.delivery.svc.documentacknoledgement;

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

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@Slf4j
public class DocumentAcknoledgementService {

	private final S3Client s3client;

	@Autowired
	public DocumentAcknoledgementService( S3Client s3Client ) {
		this.s3client = s3Client;
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
	public ResponseEntity<Resource> downloadDocument(String iun, int documentIndex) {
		log.debug( "Document download START for iun and documentIndex {}" , iun, documentIndex);
		 
		// GET BUCKET
		Bucket bucket = bucket();
		String bucketName = bucket.name();
		
		// READ FILES / DIRECTORY
		String keyPrefix = iun + "/legalfacts/";
		
		List<S3Object> s3ObjectList = s3ObjectList(bucketName, keyPrefix);
		
		if ( documentIndex >= s3ObjectList.size() ) {
			log.warn( "Document ndex out of bound for iun: " + iun + " and documentIndex: " + documentIndex );
			throw new PnInternalException( "Document ndex out of bound for iun: " + iun + " and documentIndex: " + documentIndex );
		}
		
		// GET DOCUMENT KEY BY DOCUMENT INDEX
		String documentKey = s3ObjectList.get( documentIndex ).key();
		
		ResponseInputStream<GetObjectResponse> s3Object = s3Object(bucketName, documentKey);	
		
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

	private Bucket bucket() {
		List<Bucket> buckets =	s3client.listBuckets().buckets();
		Bucket bucket = buckets.get( 0 );
		return bucket;
	}
	
	private List<S3Object> s3ObjectList(String bucketName, String keyPrefix) {
		ListObjectsResponse objectResponseList = s3client.listObjects( ListObjectsRequest.builder()
																.bucket( bucketName )
																.prefix( keyPrefix )
																.build() );
		
		List<S3Object> s3ObjectList = objectResponseList.contents();
		return s3ObjectList;
	}

	private ResponseInputStream<GetObjectResponse> s3Object(String bucketName, String documentKey) {
		GetObjectRequest s3ObjectRequest = GetObjectRequest.builder()
											.bucket(bucketName)
											.key( documentKey )
											.build();
		
		ResponseInputStream<GetObjectResponse> s3Object = s3client.getObject( s3ObjectRequest );
		return s3Object;
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

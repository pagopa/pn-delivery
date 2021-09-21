package it.pagopa.pn.delivery.svc.receivenotification;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttachmentService {

    private static final String SHA256_METADATA_NAME = "sha256";
	private static final String CONTENT_TYPE_METADATA_NAME = "content-type";
	private final FileStorage fileStorage;

    public AttachmentService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public Notification saveAttachments(Notification notification ) {
        String iun = notification.getIun();

        Notification.NotificationBuilder builder = notification.toBuilder();

        // - Save documents
        log.debug("Saving documents for iun {} START", iun);
        saveDocuments( notification, iun, builder );
        log.debug("Saving documents for iun {} END", iun);

        // - save F24
        log.debug("Saving F24 for iun {} START", iun);
        saveF24( notification, iun, builder);
        log.debug("Saving F24 for iun {} END", iun);
        return builder.build();
    }

    private void saveDocuments(Notification notification, String iun, Notification.NotificationBuilder builder) {
        AtomicInteger index = new AtomicInteger( 0 );
        builder.documents( notification.getDocuments().stream()
                .map( toSave -> {
                    String key = String.format("%s/documents/%d", iun, index.getAndIncrement() );
                    return saveAndUpdateAttachment( iun, toSave, key );
                })
                .collect(Collectors.toList())
        );
    }
    
    public ResponseEntity<Resource> loadDocument(String iun, int documentIndex) {
    	String documentKey = String.format("%s/documents/%d", iun, documentIndex );
    	
    	FileData fileData = fileStorage.getFileByKey( documentKey );
    			
		ResponseEntity<Resource> response = ResponseEntity.ok()
	            .headers( headers() )
	            .contentLength( fileData.getContentLength() )
	            .contentType( extractMediaType( fileData.getMetadata() ) )
	            .body( new InputStreamResource (fileData.getContent() ) );
		
		log.debug("downloadDocument: response {}", response);
	    return response;
    }

	private MediaType extractMediaType(Map<String, String> metadata ) {
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		String contentType = metadata.get( CONTENT_TYPE_METADATA_NAME );
				
		try {
			if ( StringUtils.isNotBlank( contentType ) ) {
				mediaType = MediaType.parseMediaType( contentType );
			}
		} catch (InvalidMediaTypeException exc)  {
			// using default
		}
		return mediaType;
	}
    
    private HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
        headers.add( "Cache-Control", "no-cache, no-store, must-revalidate" );
        headers.add( "Pragma", "no-cache" );
        headers.add( "Expires", "0" );
		return headers;
	}

    private void saveF24(Notification notification, String iun, Notification.NotificationBuilder builder) {
        NotificationPaymentInfo paymentsInfo = notification.getPayment();
        if( paymentsInfo != null ) {

            NotificationPaymentInfo.NotificationPaymentInfoBuilder paymentsBuilder;
            paymentsBuilder = notification.getPayment().toBuilder();

            NotificationPaymentInfo.F24 f24 = paymentsInfo.getF24();
            if( f24 != null ) {

                NotificationPaymentInfo.F24.F24Builder f24Builder = f24.toBuilder();

                NotificationAttachment f24FlatRate = f24.getFlatRate();
                if( f24FlatRate != null ) {
                    String key = String.format( "%s/f24/flatRate", iun);
                    f24Builder.flatRate( saveAndUpdateAttachment(iun, f24FlatRate, key ) );
                }

                NotificationAttachment f24Digital = f24.getDigital();
                if( f24Digital != null ) {
                    String key = String.format( "%s/f24/digital", iun);
                    f24Builder.digital( saveAndUpdateAttachment(iun, f24Digital, key ) );
                }

                NotificationAttachment f24Analog = f24.getAnalog();
                if( f24Analog != null ) {
                    String key = String.format( "%s/f24/analog", iun);
                    f24Builder.analog( saveAndUpdateAttachment(iun, f24Analog, key ) );
                }

                paymentsBuilder.f24( f24Builder.build() );
            }
            builder.payment( paymentsBuilder.build() );
        }
    }

    private NotificationAttachment saveAndUpdateAttachment(String iun, NotificationAttachment attachment, String key) {
        log.debug("Saving attachment: iun={} key={}", iun, key);
        String versionId = saveOneAttachmentToFileStorage( key, attachment );
        log.info("SAVED attachment iun={} key={} versionId={}", iun, key, versionId);
        return updateSavedAttachment( attachment, versionId );
    }

    private NotificationAttachment updateSavedAttachment( NotificationAttachment attachment, String versionId ) {
        return attachment.toBuilder()
                .savedVersionId(
                        versionId
                )
                .build();
    }

    /**
     *
     * @param key the path where to save the <code>attachment</code>
     * @param attachment the file to save
     * @return the versionId of the saved file
     */
    private String saveOneAttachmentToFileStorage( String key, NotificationAttachment attachment ) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(CONTENT_TYPE_METADATA_NAME, attachment.getContentType() );
        metadata.put(SHA256_METADATA_NAME, attachment.getDigests().getSha256() );

        byte[] body = Base64.getDecoder().decode( attachment.getBody() );

        return fileStorage.putFileVersion( key, new ByteArrayInputStream( body ), body.length, metadata );
    }
}

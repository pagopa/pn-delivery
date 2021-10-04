package it.pagopa.pn.delivery.svc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons_delivery.utils.LegalfactsMetadataUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfo;
import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AttachmentService {

    private static final String SHA256_METADATA_NAME = "sha256";
	
	private final FileStorage fileStorage;
	private final LegalfactsMetadataUtils legalfactMetadataUtils;
	private final NotificationReceiverValidator validator;

    public AttachmentService(FileStorage fileStorage, LegalfactsMetadataUtils legalfactMetadataUtils, NotificationReceiverValidator validator) {
        this.fileStorage = fileStorage;
        this.legalfactMetadataUtils = legalfactMetadataUtils;
        this.validator = validator;
    }

    public String buildPreloadFullKey( String paId, String key) {
        return "preload/" + paId + "/" + key;
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
                    return saveAndUpdateAttachment( notification, toSave, key );
                })
                .collect(Collectors.toList())
        );
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
                    f24Builder.flatRate( saveAndUpdateAttachment(notification, f24FlatRate, key ) );
                }

                NotificationAttachment f24Digital = f24.getDigital();
                if( f24Digital != null ) {
                    String key = String.format( "%s/f24/digital", iun);
                    f24Builder.digital( saveAndUpdateAttachment(notification, f24Digital, key ) );
                }

                NotificationAttachment f24Analog = f24.getAnalog();
                if( f24Analog != null ) {
                    String key = String.format( "%s/f24/analog", iun);
                    f24Builder.analog( saveAndUpdateAttachment(notification, f24Analog, key ) );
                }

                paymentsBuilder.f24( f24Builder.build() );
            }
            builder.payment( paymentsBuilder.build() );
        }
    }
    
    private NotificationAttachment saveAndUpdateAttachment(Notification notification, NotificationAttachment attachment, String key) {
        String iun = notification.getIun();
        String paId = notification.getSender().getPaId();
        log.debug("Saving attachment: iun={} key={}", iun, key);

        NotificationAttachment updatedAttachment;
        if( attachment.getRef() == null ) {
            String versionId = saveOneAttachmentToFileStorage( key, attachment );
            log.info("SAVED attachment iun={} key={} versionId={}", iun, key, versionId);
            updatedAttachment = updateSavedAttachment( attachment, versionId, key );
        }
        else {
            updatedAttachment = attachment.toBuilder()
                    .ref( attachment.getRef().toBuilder()
                            .key( buildPreloadFullKey( paId, attachment.getRef().getKey()) )
                            .build()
                    )
                    .build();
            checkAttachmentDigests( updatedAttachment );
        }
        return updatedAttachment;
    }

    /**
     *
     * @param key the path where to save the <code>attachment</code>
     * @param attachment the file to save
     * @return the versionId of the saved file
     */
    private String saveOneAttachmentToFileStorage( String key, NotificationAttachment attachment ) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SHA256_METADATA_NAME, attachment.getDigests().getSha256() );

        byte[] body = Base64.getDecoder().decode( attachment.getBody() );

        return fileStorage.putFileVersion( key, new ByteArrayInputStream( body ), body.length, attachment.getContentType(), metadata );
    }

    private NotificationAttachment updateSavedAttachment( NotificationAttachment attachment, String versionId, String fullKey ) {
        return attachment.toBuilder()
                .ref( NotificationAttachment.Ref.builder()
                        .key( fullKey )
                        .versionToken( versionId )
                        .build()
                )
                .build();
    }

    private void checkAttachmentDigests(NotificationAttachment attachment) {
        NotificationAttachment.Ref attachmentRef = attachment.getRef();

        try {
            FileData fd = fileStorage.getFileVersion(
                    attachmentRef.getKey(), attachmentRef.getVersionToken() );

            String actualSha256 = DigestUtils.sha256Hex( fd.getContent() );
            validator.checkPreloadedDigests(
                    attachmentRef.getKey(),
                    attachment.getDigests(),
                    NotificationAttachment.Digests.builder()
                            .sha256( actualSha256 )
                            .build()
                );

        } catch (IOException exc) {
            throw new PnInternalException("Checking sha256 of " + attachmentRef, exc );
        }
    }


    public ResponseEntity<Resource> loadAttachment(NotificationAttachment.Ref attachmentRef) {
        String attachmentKey = attachmentRef.getKey();
        String savedVersionId = attachmentRef.getVersionToken();

        FileData fileData = fileStorage.getFileVersion( attachmentKey, savedVersionId );

        ResponseEntity<Resource> response = ResponseEntity.ok()
                .headers( headers() )
                .contentLength( fileData.getContentLength() )
                .contentType( extractMediaType( fileData.getContentType() ) )
                .body( new InputStreamResource (fileData.getContent() ) );

        log.debug("AttachmentKey: response {}", response);
        return response;
    }

    private MediaType extractMediaType( String contentType ) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

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


	public List<LegalFactsListEntry> listNotificationLegalFacts(String iun) {

        String prefix = legalfactMetadataUtils.baseKey(iun);
		List<FileData> files = fileStorage.getDocumentsListing( prefix );

		return files.stream().map( legalfactMetadataUtils::fromFileData )
                .collect(Collectors.toList());
	}

    public ResponseEntity<Resource> loadLegalfact(String iun, String legalfactId ) {
        NotificationAttachment.Ref ref = legalfactMetadataUtils.fromIunAndLegalFactId( iun, legalfactId );
        return loadAttachment( ref );
    }


}

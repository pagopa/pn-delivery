package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDocument;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttachmentService {

    private static final String SHA256_METADATA_NAME = "sha256";

    private final FileStorage fileStorage;

    public AttachmentService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public String buildPreloadFullKey( String paId, String key) {
        return "preload/" + paId + "/" + key;
    }

    public InternalNotification saveAttachments(InternalNotification internalNotification) {
        String iun = internalNotification.getIun();

        //InternalNotification.NotificationBuilder builder = internalNotification.toBuilder();

        // - Save documents
        log.debug("Saving documents for iun={} START", iun);
        saveDocuments(internalNotification, iun);
        log.debug("Saving documents for iun={} END", iun);

        // - save F24
        log.debug("Saving F24 for iun={} START", iun);
        for ( NotificationRecipient recipient : internalNotification.getRecipients() ) {
            //saveF24( recipient, iun, builder);
        }

        log.debug("Saving F24 for iun={} END", iun);
        //return builder.build();
        return null;
    }

    /*private void saveDocuments(InternalNotification internalNotification, String iun, InternalNotification.NotificationBuilder builder) {
        AtomicInteger index = new AtomicInteger( 0 );
        builder.documents( internalNotification.getDocuments().stream()
                .map( toSave -> {
                    String key = String.format("%s/documents/%d", iun, index.getAndIncrement() );
                    return saveAndUpdateAttachment(internalNotification, toSave, key );
                })
                .collect(Collectors.toList())
        );
    }*/

    private void saveDocuments(InternalNotification internalNotification, String iun) {
        AtomicInteger index = new AtomicInteger( 0 );
        List<NotificationDocument> documents = internalNotification.getDocuments();
        for ( NotificationDocument document : documents ) {
            String key = String.format("%s/documents/%d", iun, index.getAndIncrement() );
            NotificationAttachmentBodyRef returnBodyRef = saveAndUpdateAttachment( internalNotification, document.getRef(), key );
        }
    }

    /*private void saveF24(NotificationRecipient recipient, String iun, InternalNotification.NotificationBuilder builder) {
        NotificationPaymentInfo paymentsInfo = recipient.getPayment();
        if( paymentsInfo != null ) {

            NotificationPaymentInfo paymentsBuilder = recipient.getPayment();

            NotificationPaymentInfo.F24 f24 = paymentsInfo..getF24();
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
    }*/
    
    private NotificationAttachmentBodyRef saveAndUpdateAttachment(InternalNotification internalNotification, NotificationAttachmentBodyRef ref, String key) {
        String iun = internalNotification.getIun();
        String paId = internalNotification.getSender().getPaId();
        log.info("Saving attachment: iun={} key={}", iun, key);

        //NotificationAttachment.Ref attachmentRef = attachment.getRef();

        NotificationAttachmentBodyRef updatedAttachment;

        log.info( "Retrieve attachment by ref iun={} key={}", iun, key );
        String fullKey = buildPreloadFullKey( paId, ref.getKey());
        String versionId = ref.getVersionToken();

        FileData fd = fileStorage.getFileVersion( fullKey, ref.getVersionToken() );
        try {
            fd.getContent().close();
        } catch (IOException e) {
            String msg = "Error closing attachment Ref with key=" + ref.getKey()  + " version=" + fd.getVersionId();
            log.error( msg );
            throw new PnInternalException( msg, e );
        }

        updatedAttachment = updateAttachmentMetadata( ref, versionId, fullKey, fd.getContentType() );

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

    private NotificationAttachmentBodyRef updateAttachmentMetadata(NotificationAttachmentBodyRef attachment,
                                                            String versionId, String fullKey, String contentType ) {

                attachment.setKey( fullKey );
                attachment.setVersionToken( versionId );
                return attachment;
                //.ref( NotificationAttachment.Ref.builder()
                        //.key( fullKey )
                        //.versionToken( versionId )
                        //.build()
                //)
                //.contentType( contentType )
                //.build();
    }
}

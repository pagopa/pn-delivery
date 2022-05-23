package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.abstractions.FileData;
import it.pagopa.pn.commons.abstractions.FileStorage;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;

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

    private final FileStorage fileStorage;

    public AttachmentService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public String buildPreloadFullKey( String paId, String key) {
        return "preload/" + paId + "/" + key;
    }

    public InternalNotification saveAttachments(InternalNotification internalNotification) {
        String iun = internalNotification.getIun();

        // - Save documents
        log.debug("Saving documents for iun={} START", iun);
        saveDocuments(internalNotification);
        log.debug("Saving documents for iun={} END", iun);

        // - save F24
        log.debug("Saving F24 for iun={} START", iun);
        saveF24( internalNotification );
        log.debug("Saving F24 for iun={} END", iun);

        return internalNotification;
    }

    private void saveDocuments(InternalNotification internalNotification) {
        /*AtomicInteger index = new AtomicInteger( 0 );
        internalNotification.documents( internalNotification.getDocuments().stream()
                .map( toSave -> {
                    //String key = String.format("%s/documents/%d", iun, index.getAndIncrement() );
                    return saveAndUpdateAttachment(internalNotification, toSave, null);
                })
                .collect(Collectors.toList())
        );*/
    }

    private void saveF24(InternalNotification notification) {

        /*for ( NotificationRecipient recipient : notification.getRecipients() ) {
            NotificationPaymentInfo paymentsInfo = recipient.getPayment();

            if( paymentsInfo != null ) {
                NotificationPaymentAttachment f24FlateRate = paymentsInfo.getF24flatRate();
                if (f24FlateRate != null ) {
                    saveAndUpdatePaymentAttachment(f24FlateRate, notification.getSenderPaId());
                }
                NotificationPaymentAttachment f24White = paymentsInfo.getF24standard();
                if (f24White != null) {
                    saveAndUpdatePaymentAttachment(f24White, notification.getSenderPaId());
                }
                NotificationPaymentAttachment pagoPaForm = paymentsInfo.getPagoPaForm();
                if (pagoPaForm != null) {
                    saveAndUpdatePaymentAttachment(pagoPaForm, notification.getSenderPaId());
                }
            }
        }*/
    }

    private void saveAndUpdatePaymentAttachment(NotificationPaymentAttachment paymentAttachment, String paId) {
        /*NotificationAttachmentBodyRef attachmentRef = paymentAttachment.getRef();

        log.info( "Retrieve attachment by ref with key={}", attachmentRef.getKey() );
        String fullKey = buildPreloadFullKey( paId, attachmentRef.getKey());
        String versionId = attachmentRef.getVersionToken();

        FileData fd = fileStorage.getFileVersion( fullKey, attachmentRef.getVersionToken() );
        try {
            fd.getContent().close();
        } catch (IOException e) {
            String msg = "Error closing attachment Ref with key=" + attachmentRef.getKey()  + " version=" + fd.getVersionId();
            log.error( msg );
            throw new PnInternalException( msg, e );
        }

        paymentAttachment.contentType( fd.getContentType() );
        paymentAttachment.getRef().setVersionToken( versionId );
        paymentAttachment.getRef().setKey( fullKey );*/
    }

    private NotificationDocument saveAndUpdateAttachment(InternalNotification internalNotification, NotificationDocument notificationDocument, String key) {
        String iun = internalNotification.getIun();
        String paId = internalNotification.getSenderPaId();
        //log.info("Saving attachment: iun={} key={}", iun, key);

        NotificationAttachmentBodyRef attachmentRef = notificationDocument.getRef();

        NotificationDocument updatedAttachment;

        log.info( "Retrieve attachment by ref iun={} key={}", iun, attachmentRef.getKey() );
        String fullKey = buildPreloadFullKey( paId, attachmentRef.getKey());
        String versionId = attachmentRef.getVersionToken();

        FileData fd = fileStorage.getFileVersion( fullKey, attachmentRef.getVersionToken() );
        try {
            fd.getContent().close();
        } catch (IOException e) {
            String msg = "Error closing attachment Ref with key=" + attachmentRef.getKey()  + " version=" + fd.getVersionId();
            log.error( msg );
            throw new PnInternalException( msg, e );
        }

        updatedAttachment = updateAttachmentMetadata( notificationDocument, versionId, fullKey, fd.getContentType() );

        return updatedAttachment;
    }

    /**
     *
     * @param attachment the path where to save the <code>attachment</code>
     * @param attachment the file to save
     * @return the versionId of the saved file
     */
    /*private String saveOneAttachmentToFileStorage( String key, NotificationAttachment attachment ) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SHA256_METADATA_NAME, attachment.getDigests().getSha256() );

        byte[] body = Base64.getDecoder().decode( attachment.getBody() );

        return fileStorage.putFileVersion( key, new ByteArrayInputStream( body ), body.length, attachment.getContentType(), metadata );
    }*/

    private NotificationDocument updateAttachmentMetadata(NotificationDocument attachment,
                                                            String versionId, String fullKey, String contentType ) {
        return attachment
                .ref( NotificationAttachmentBodyRef.builder()
                        .key( fullKey )
                        .versionToken( versionId )
                        .build()
                )
                .contentType( contentType );
    }
}

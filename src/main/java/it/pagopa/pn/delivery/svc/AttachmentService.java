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

        InternalNotification.InternalNotificationBuilder builder = internalNotification.toBuilder();

        // - Save documents
        log.debug("Saving documents for iun={} START", iun);
        saveDocuments(internalNotification, iun, builder);
        log.debug("Saving documents for iun={} END", iun);

        // - save F24
        log.debug("Saving F24 for iun={} START", iun);
        for ( NotificationRecipient recipient : internalNotification.getRecipients() ) {
            saveF24( recipient, iun, builder);
        }

        log.debug("Saving F24 for iun={} END", iun);
        //return builder.build();
        return null;
    }

    private void saveDocuments(InternalNotification internalNotification, String iun, InternalNotification.InternalNotificationBuilder builder) {
        AtomicInteger index = new AtomicInteger( 0 );
        builder.documents( internalNotification.getDocuments().stream()
                .map( toSave -> {
                    String key = String.format("%s/documents/%d", iun, index.getAndIncrement() );
                    return saveAndUpdateAttachment(internalNotification, toSave, key );
                })
                .collect(Collectors.toList())
        );
    }

    private void saveF24(NotificationRecipient recipient, String iun, InternalNotification.InternalNotificationBuilder builder) {
        NotificationPaymentInfo paymentsInfo = recipient.getPayment();
        if( paymentsInfo != null ) {

            /*NotificationPaymentInfo.NotificationPaymentInfoBuilder paymentsBuilder;
            paymentsBuilder = recipient.getPayment().toBuilder();

            //NotificationPaymentInfo f24 = paymentsInfo.f24analogRaccDouble();
            if( f24 != null ) {

                NotificationPaymentInfo.F24.F24Builder f24Builder = f24.toBuilder();

                NotificationPaymentAttachment f24FlatRate = f24.getFlatRate();
                if( f24FlatRate != null ) {
                    String key = String.format( "%s/f24/flatRate", iun);
                    f24Builder.flatRate( saveAndUpdateAttachment(notification, f24FlatRate, key ) );
                }

                NotificationPaymentAttachment f24Digital = f24.getDigital();
                if( f24Digital != null ) {
                    String key = String.format( "%s/f24/digital", iun);
                    f24Builder.digital( saveAndUpdateAttachment(notification, f24Digital, key ) );
                }

                NotificationPaymentAttachment f24Analog = f24.getAnalog();
                if( f24Analog != null ) {
                    String key = String.format( "%s/f24/analog", iun);
                    f24Builder.analog( saveAndUpdateAttachment(notification, f24Analog, key ) );
                }

                paymentsBuilder.f24( f24Builder.build() );
            }
            builder.payment( paymentsBuilder.build() );*/
        }
    }

    private NotificationDocument saveAndUpdateAttachment(InternalNotification internalNotification, NotificationDocument notificationDocument, String key) {
        String iun = internalNotification.getIun();
        String paId = internalNotification.getSenderPaId();
        log.info("Saving attachment: iun={} key={}", iun, key);

        NotificationAttachmentBodyRef attachmentRef = notificationDocument.getRef();

        NotificationDocument updatedAttachment;

        log.info( "Retrieve attachment by ref iun={} key={}", iun, key );
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

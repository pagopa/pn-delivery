package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationAttachmentService {

    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PRELOADED = "PRELOADED";

    private enum ATTACHMENT_TYPE {
        PAGOPA("PAGOPA"),
        F24("F24"),
        F24_FLAT("F24_FLAT"),
        F24_STANDARD("F24_STANDARD");
        private final String value;

        ATTACHMENT_TYPE(String value) {
            this.value = value;
        }
    }

    private final PnSafeStorageClientImpl safeStorageClient;
    private final NotificationDao notificationDao;
    private final PnMandateClientImpl pnMandateClient;
    private final CheckAuthComponent checkAuthComponent;

    public NotificationAttachmentService(PnSafeStorageClientImpl safeStorageClient, NotificationDao notificationDao, PnMandateClientImpl pnMandateClient, CheckAuthComponent checkAuthComponent) {
        this.safeStorageClient = safeStorageClient;
        this.notificationDao = notificationDao;
        this.pnMandateClient = pnMandateClient;
        this.checkAuthComponent = checkAuthComponent;
    }

    private FileDownloadResponse getFile(String fileKey){
        log.info("getFile with fileKey={} ", fileKey);
        return this.safeStorageClient.getFile(fileKey, false);
    }

    public List<PreLoadResponse> preloadDocuments(List<PreLoadRequest> preLoadRequests){
        return preLoadRequests.stream().map(req -> {
            log.info("preloadDocuments contentType:{} preloadIdx:{}", req.getContentType(), req.getPreloadIdx());
            FileCreationRequest fileCreationRequest = new FileCreationRequest();
            fileCreationRequest.setContentType(req.getContentType());
            fileCreationRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
            fileCreationRequest.setStatus(PRELOADED);

            var resp = this.safeStorageClient.createFile(fileCreationRequest, req.getSha256());
            return  PreLoadResponse.builder()
                    .url(resp.getUploadUrl())
                    .key(resp.getKey())
                    .httpMethod(PreLoadResponse.HttpMethodEnum.fromValue(resp.getUploadMethod().getValue()))
                    .secret(resp.getSecret())
                    .preloadIdx(req.getPreloadIdx())
                    .build();
        }).collect(Collectors.toList());
    }

    public static class FileDownloadIdentify {
        private Integer documentIdx;
        private Integer recipientIdx;
        private String attachmentName;

        private FileDownloadIdentify() {}

        public FileDownloadIdentify(Integer documentIdx) {
            this.documentIdx = documentIdx;
        }

        public FileDownloadIdentify(Integer recipientIdx, String attachmentName) {
            this.recipientIdx = recipientIdx;
            this.attachmentName = attachmentName;
        }

        public static FileDownloadIdentify create(Integer documentIndex, Integer recipientIdx, String attachmentName) {
            if (documentIndex != null) {
                return new FileDownloadIdentify( documentIndex );
            } else {
                return new FileDownloadIdentify( recipientIdx, attachmentName );
            }
        }
    }

    public static class FileKeyAndName{
        private String fileKey;
        private String fileName;

        public FileKeyAndName(String fileKey, String fileName) {
            this.fileKey = fileKey;
            this.fileName = fileName;
        }
    }

    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(
            String iun,
            String cxType,
            String xPagopaPnCxId,
            String mandateId,
            Integer documentIdx) {
        return downloadDocumentWithRedirect(cxType, xPagopaPnCxId, mandateId, iun, documentIdx, null, null);
    }


    public NotificationAttachmentDownloadMetadataResponse downloadAttachmentWithRedirect(
            String iun,
            String cxType,
            String xPagopaPnCxId,
            String mandateId,
            Integer recipientIdx,
            String attachmentName) {
        return downloadDocumentWithRedirect(cxType, xPagopaPnCxId, mandateId, iun, null, recipientIdx, attachmentName);
    }

    private NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(
            String cxType,
            String cxId,
            String mandateId,
            String iun,
            Integer documentIndex,
            Integer recipientIdx,
            String attachmentName) {
        log.info("downloadDocumentWithRedirect for cxType={} iun={} documentIndex={} recipientIdx={} xPagopaPnCxId={} attachmentName={} mandateId={}", cxType, iun, documentIndex, recipientIdx, cxId, attachmentName, mandateId );

        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, cxId, mandateId, iun, recipientIdx );



        Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            InternalNotification notification = optNotification.get();


            AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );

            Integer downloadRecipientIdx = handleReceiverAttachmentDownload( recipientIdx, authorizationOutcome.getEffectiveRecipientIdx(), documentIndex );
            FileDownloadIdentify fileDownloadIdentify = FileDownloadIdentify.create( documentIndex, downloadRecipientIdx, attachmentName );
            if ( !authorizationOutcome.isAuthorized() ) {
                log.error("Error download attachment. xPagopaPnCxId={} mandateId={} recipientIdx={} cannot download attachment for notification with iun={}", cxId, mandateId, recipientIdx, iun);
                throw new PnNotFoundException("Notification not found for iun=" + iun);
            }

            FileKeyAndName fileKeyAndName = computeFileInfo( fileDownloadIdentify, notification );
            String fileKey = fileKeyAndName.fileKey;
            String fileName = fileKeyAndName.fileName;

            log.info("downloadDocumentWithRedirect with fileKey={} filename:{} ", fileKey, fileName);
            FileDownloadResponse r = this.getFile(fileKey);
            return NotificationAttachmentDownloadMetadataResponse.builder()
                    .filename( fileName)
                    .url( r.getDownload().getUrl() )
                    .contentLength(r.getContentLength())
                    .contentType( r.getContentType() )
                    .sha256( r.getChecksum() )
                    .retryAfter(r.getDownload().getRetryAfter())
                    .build();
        } else {
            log.error("downloadDocumentWithRedirect Notification not found for iun={}", iun);
            throw new PnNotFoundException("Notification not found for iun=" + iun);
        }
    }

    private Integer handleReceiverAttachmentDownload(Integer recipientIdx, Integer effectiveRecipientIdx, Integer documentIdx) {
        // - Se è stato richiesto il download di un documento ...
        if (documentIdx != null ) {
            // ... non serve il recipientIdx documenti associati alla notifica non al destinatario
            return null;
        } else {
            // - Altrimenti per esclusione stiamo scaricando un attachment.
            // - In tal caso solo il destinatario può non specificare il parametro recipientIdx nella chiamata ...
            if ( recipientIdx == null ) {
                // ... lo evinciamo dalle informazioni di autenticazione.
                return effectiveRecipientIdx;
            } else {
                // - E' stato richiesto download di un attachment da un mittente che è obbligato a specificare il recipienIdx
                return recipientIdx;
            }
        }


    }

    public FileKeyAndName computeFileInfo(FileDownloadIdentify fileDownloadIdentify, InternalNotification notification ) {
        String fileName;
        String fileKey;

        String iun = notification.getIun();
        Integer documentIndex = fileDownloadIdentify.documentIdx;
        if (documentIndex != null)
        {
            NotificationDocument doc = notification.getDocuments().get( documentIndex );
            fileName = buildFilename(iun, doc.getTitle());
            fileKey = doc.getRef().getKey();
        }
        else
        {
            String attachmentName = fileDownloadIdentify.attachmentName;
            NotificationRecipient effectiveRecipient = notification.getRecipients().get( fileDownloadIdentify.recipientIdx );
            fileKey = getFileKeyOfAttachment(iun, effectiveRecipient, attachmentName, notification.getNotificationFeePolicy());
            fileName = buildFilename(iun, attachmentName);
        }
        return new FileKeyAndName( fileKey, fileName );
    }

    private String getFileKeyOfAttachment(String iun, NotificationRecipient doc, String attachmentName, FullSentNotification.@NotNull NotificationFeePolicyEnum notificationFeePolicy){
        switch (ATTACHMENT_TYPE.valueOf(attachmentName))
        {
            case PAGOPA:
                return doc.getPayment().getPagoPaForm().getRef().getKey();
            case F24:
                if (notificationFeePolicy== FullSentNotification.NotificationFeePolicyEnum.FLAT_RATE)
                    return doc.getPayment().getF24flatRate().getRef().getKey();
                else
                    return doc.getPayment().getF24standard().getRef().getKey();
            case F24_FLAT:
                return doc.getPayment().getF24flatRate().getRef().getKey();
            case F24_STANDARD:
                return doc.getPayment().getF24standard().getRef().getKey();
        }

        log.error("NotificationRecipient invalid attachmentname attachmentName={}", attachmentName);
        throw new PnInternalException("NotificationRecipient invalid attachmentName for iun=" + iun);
    }

    private String buildFilename(String iun, String name){
        String unescapedFileName = iun + "__" + name;
        return unescapedFileName.replaceAll( "[^A-Za-z0-9-_]", "_" ) + ".pdf";
    }
}

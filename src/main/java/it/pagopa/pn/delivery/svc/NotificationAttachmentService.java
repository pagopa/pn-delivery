package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MimeTypesUtils;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_FILEINFONOTFOUND;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONWITHOUTPAYMENTATTACHMENT;

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
    private final CheckAuthComponent checkAuthComponent;
    private final NotificationViewedProducer notificationViewedProducer;
    private final MVPParameterConsumer mvpParameterConsumer;

    public NotificationAttachmentService(PnSafeStorageClientImpl safeStorageClient, NotificationDao notificationDao, CheckAuthComponent checkAuthComponent, NotificationViewedProducer notificationViewedProducer, MVPParameterConsumer mvpParameterConsumer) {
        this.safeStorageClient = safeStorageClient;
        this.notificationDao = notificationDao;
        this.checkAuthComponent = checkAuthComponent;
        this.notificationViewedProducer = notificationViewedProducer;
        this.mvpParameterConsumer = mvpParameterConsumer;
    }

    public FileDownloadResponse getFile(String fileKey){
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

    public static class FileInfos {
        private final String fileName;
        private final FileDownloadResponse fileDownloadResponse;

        public FileInfos(String fileName, FileDownloadResponse fileDownloadResponse) {
            this.fileName = fileName;
            this.fileDownloadResponse = fileDownloadResponse;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(
            String iun,
            String cxType,
            String xPagopaPnCxId,
            String mandateId,
            Integer documentIdx,
            Boolean markNotificationAsViewed) {
        return downloadDocumentWithRedirect(cxType, xPagopaPnCxId, mandateId, iun, documentIdx, null, null, markNotificationAsViewed);
    }


    public NotificationAttachmentDownloadMetadataResponse downloadAttachmentWithRedirect(
            String iun,
            String cxType,
            String xPagopaPnCxId,
            String mandateId,
            Integer recipientIdx,
            String attachmentName,
            Boolean markNotificationAsViewed) {
        return downloadDocumentWithRedirect(cxType, xPagopaPnCxId, mandateId, iun, null, recipientIdx, attachmentName, markNotificationAsViewed);
    }

    private NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(
            String cxType,
            String cxId,
            String mandateId,
            String iun,
            Integer documentIndex,
            Integer recipientIdx,
            String attachmentName,
            Boolean markNotificationAsViewed) {
        log.info("downloadDocumentWithRedirect for cxType={} iun={} documentIndex={} recipientIdx={} xPagopaPnCxId={} attachmentName={} mandateId={} markNotificationAsViewed={}", cxType, iun, documentIndex, recipientIdx, cxId, attachmentName, mandateId, markNotificationAsViewed );

        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, cxId, mandateId, iun, recipientIdx );

        Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);
        if (optNotification.isPresent()) {
            InternalNotification notification = optNotification.get();

            log.info( "START check authorization" );
            AuthorizationOutcome authorizationOutcome = checkAuthComponent.canAccess( readAccessAuth, notification );
            log.info( "END check authorization autorized={} xPagopaPnCxId={} mandateId={} recipientIdx={} iun={}", authorizationOutcome.isAuthorized(), cxId, mandateId, recipientIdx, iun);

            if ( !authorizationOutcome.isAuthorized() ) {
                log.error("Error download attachment. xPagopaPnCxId={} mandateId={} recipientIdx={} cannot download attachment for notification with iun={}", cxId, mandateId, recipientIdx, iun);
                throw new PnNotificationNotFoundException("Notification not found for iun=" + iun);
            }

            Integer downloadRecipientIdx = handleReceiverAttachmentDownload( recipientIdx, authorizationOutcome.getEffectiveRecipientIdx(), documentIndex );
            FileDownloadIdentify fileDownloadIdentify = FileDownloadIdentify.create( documentIndex, downloadRecipientIdx, attachmentName );

            FileInfos fileInfos = computeFileInfo( fileDownloadIdentify, notification );

            // controlli per essere certi che la richiesta è stata fatta da un destinatario o da un delegato
            // ma non da rete RADD e non da mittente
            if( !cxType.equals( CxTypeAuthFleet.PA.getValue() ) && Boolean.TRUE.equals(markNotificationAsViewed) ) {
                notificationViewedProducer.sendNotificationViewed( iun, Instant.now(), authorizationOutcome.getEffectiveRecipientIdx() );
            }

            return NotificationAttachmentDownloadMetadataResponse.builder()
                    .filename( fileInfos.fileName)
                    .url( fileInfos.fileDownloadResponse.getDownload().getUrl() )
                    .contentLength( nullSafeBigDecimalToInteger(
                            fileInfos.fileDownloadResponse.getContentLength()
                    ))
                    .contentType( fileInfos.fileDownloadResponse.getContentType() )
                    .sha256( fileInfos.fileDownloadResponse.getChecksum() )
                    .retryAfter( nullSafeBigDecimalToInteger(
                            fileInfos.fileDownloadResponse.getDownload().getRetryAfter()
                    ))
                    .build();
        } else {
            log.error("downloadDocumentWithRedirect Notification not found for iun={}", iun);
            throw new PnNotificationNotFoundException("Notification not found for iun=" + iun);
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

    public FileInfos computeFileInfo(FileDownloadIdentify fileDownloadIdentify, InternalNotification notification ) {
        String fileName;
        String fileKey;

        String iun = notification.getIun();
        Integer documentIndex = fileDownloadIdentify.documentIdx;
        String name;
        if (documentIndex != null)
        {
            NotificationDocument doc = notification.getDocuments().get( documentIndex );
            name   = doc.getTitle();
            fileKey = doc.getRef().getKey();
        }
        else
        {
            String attachmentName = fileDownloadIdentify.attachmentName;
            NotificationRecipient effectiveRecipient = notification.getRecipients().get( fileDownloadIdentify.recipientIdx );
            fileKey = getFileKeyOfAttachment(iun, effectiveRecipient, attachmentName, mvpParameterConsumer.isMvp(notification.getSenderTaxId()));
            if (!StringUtils.hasText( fileKey )) {
                String exMessage = String.format("Unable to find key for attachment=%s iun=%s with this paymentInfo=%s", attachmentName, iun, effectiveRecipient.getPayment().toString());
                throw new PnNotFoundException("FileInfo not found", exMessage, ERROR_CODE_DELIVERY_FILEINFONOTFOUND);
            }
            name = attachmentName;
        }

        log.info("downloadDocumentWithRedirect with fileKey={} name:{} - iun={}", fileKey, name, iun);
        try {
            FileDownloadResponse r = this.getFile(fileKey);
            fileName = buildFilename(iun, name, r.getContentType());

            log.info("downloadDocumentWithRedirect with fileKey={} filename:{} - iun={}", fileKey, fileName, iun);
            return new FileInfos( fileName, r );
        } catch (Exception exc) {
            if (exc instanceof PnHttpResponseException && ((PnHttpResponseException) exc).getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new PnBadRequestException("File info not found", exc.getMessage(), ERROR_CODE_DELIVERY_FILEINFONOTFOUND, exc);
            }
            throw exc;
        }
    }

    private String getFileKeyOfAttachment(String iun, NotificationRecipient doc, String attachmentName, boolean isMVPTria){
        NotificationPaymentInfo payment = doc.getPayment();
        if ( !Objects.nonNull( payment ) ) {
          String exMessage =  String.format("Notification without payment attachment - iun=%s", iun);
            log.error(exMessage);
            if(isMVPTria) {
              throw new PnInternalException(exMessage, ERROR_CODE_DELIVERY_NOTIFICATIONWITHOUTPAYMENTATTACHMENT);
            } else {
              throw new PnNotFoundException("FileInfo not found", exMessage, ERROR_CODE_DELIVERY_FILEINFONOTFOUND);              
            }
           
        }

        switch (ATTACHMENT_TYPE.valueOf(attachmentName))
        {
            case PAGOPA: {
                return getKey( payment.getPagoPaForm() );
            }
            case F24: {
                // TODO: Al momento viene restituito semplicemente l'F24 presente a seguito della issue PN-1835 MA c'è da definire la logica corretta da adottare!!
                if (doc.getPayment().getF24flatRate() != null) {
                    return getKey( payment.getF24flatRate() );
                } else {
                    return getKey( payment.getF24standard() );
                }
            }
            case F24_FLAT: {
                return getKey( payment.getF24flatRate() );
            }
            case F24_STANDARD: {
                return getKey( payment.getF24standard() );
            }
        }
        return null;
    }

    private String getKey(NotificationPaymentAttachment payment) {
        String key = null;
        if (Objects.nonNull( payment ) ) {
            key = payment.getRef().getKey();
        }
        return key;
    }

    private String buildFilename(String iun, String name, String contentType){
        String extension = "pdf";
        String defaultExtension = MimeTypesUtils.getDefaultExt(contentType);
        if( defaultExtension.equals( "unknown" ) ) {
            log.warn("right extension not found, using PDF");
        } else {
            extension = defaultExtension;
        }

        String unescapedFileName = iun + "__" + name;
        return unescapedFileName.replaceAll( "[^A-Za-z0-9-_]", "_" ) + "." + extension;
    }

    private Integer nullSafeBigDecimalToInteger(BigDecimal bd) {
        return bd != null ? bd.intValue() : null;
    }

}

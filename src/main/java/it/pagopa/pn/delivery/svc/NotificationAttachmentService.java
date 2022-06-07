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
import lombok.extern.slf4j.Slf4j;
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

    public NotificationAttachmentService(PnSafeStorageClientImpl safeStorageClient, NotificationDao notificationDao, PnMandateClientImpl pnMandateClient) {
        this.safeStorageClient = safeStorageClient;
        this.notificationDao = notificationDao;
        this.pnMandateClient = pnMandateClient;
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


    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirectByIunAndRecIdxAttachName(String iun, int recipientIdx, String attachmentName) {
       return downloadDocumentWithRedirect(iun, null, recipientIdx, null, attachmentName, null);
    }

    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirectByIunAndDocIndex(String iun, int documentIndex) {
        return downloadDocumentWithRedirect(iun, documentIndex, null, null,null,null);
    }


    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirectByIunRecUidAttachNameMandateId(String iun, String xPagopaPnCxId, String attachmentName, String mandateId) {
        return downloadDocumentWithRedirect(iun, null, null, xPagopaPnCxId, attachmentName, mandateId);
    }

    private NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(String iun, Integer documentIndex, Integer recipientIdx, String xPagopaPnCxId, String attachmentName, String mandateId ) {
        log.info("downloadDocumentWithRedirect for iun={} documentIndex={} recipientIdx={} xPagopaPnCxId={} attachmentName={} mandateId={}", iun, documentIndex, recipientIdx, xPagopaPnCxId, attachmentName, mandateId );
        Optional<InternalNotification> optNotification = notificationDao.getNotificationByIun(iun);


        if (optNotification.isPresent()) {
        	log.debug("optNotification isPresent");
            InternalNotification notification = optNotification.get();
            String fileKey;
            String fileName;
            if (documentIndex != null)
            {
            	log.debug("documentIndex is not null");
                NotificationDocument doc = notification.getDocuments().get( documentIndex );
                fileName = buildFilename(iun, doc.getTitle());
                fileKey = doc.getRef().getKey();
            }
            else
            {
            	log.debug("documentIndex is null");
                NotificationRecipient doc;
                if (recipientIdx != null)
                {
                	log.debug("recipientIdx is not null");
                    doc = recipientIdx<notification.getRecipients().size()?notification.getRecipients().get(recipientIdx):null;
                }
                else
                {
                	log.debug("recipientIdx is null");
                    String recipientId = xPagopaPnCxId;
                    if (mandateId != null)
                    {
                    	log.debug("mandateId is not null");
                        List<InternalMandateDto> mandates = this.pnMandateClient.listMandatesByDelegate(xPagopaPnCxId, mandateId);
                        if(!mandates.isEmpty()) {
                        	log.debug("mandateId is not empty");
                            recipientId = mandates.get(0).getDelegator();
                        }
                        else
                        {
                            String message = String.format("Unable to find any mandate for delegate=%s with mandateId=%s", xPagopaPnCxId, mandateId);
                            log.error( message );
                            throw new PnNotFoundException( message );
                        }
                    }

                    int idx = notification.getRecipientIds().indexOf(recipientId);
                    doc = idx>=0?notification.getRecipients().get(idx):null;
                }

                if (doc == null)
                {
                    log.error("downloadDocumentWithRedirect NotificationRecipient not found for iun={}", iun);
                    throw new PnInternalException("NotificationRecipient not found for iun=" + iun);
                }


                fileKey = getFileKeyOfAttachment(iun, doc, attachmentName, optNotification.get().getNotificationFeePolicy());
                fileName = buildFilename(iun, attachmentName);
            }

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
            throw new PnInternalException("Notification not found for iun=" + iun);
        }
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

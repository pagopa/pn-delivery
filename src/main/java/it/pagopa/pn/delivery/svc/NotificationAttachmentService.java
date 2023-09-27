package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.events.NotificationViewDelegateInfo;
import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.commons.utils.MimeTypesUtils;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.model.F24Response;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileCreationRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.model.FileDownloadResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDownloadMetadataResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PreLoadResponse;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.models.InputDownloadDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPaymentInfo;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.pnf24.PnF24ClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.authorization.AuthorizationOutcome;
import it.pagopa.pn.delivery.svc.authorization.CheckAuthComponent;
import it.pagopa.pn.delivery.svc.authorization.ReadAccessAuth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;

@Service
@Slf4j
public class NotificationAttachmentService {

    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PRELOADED = "PRELOADED";
    private static final String ATTACHMENT_TYPE_PAGO_PA = "PAGOPA" ;
    private static final String ATTACHMENT_TYPE_F24 = "F24";

    private final PnSafeStorageClientImpl safeStorageClient;
    private final PnF24ClientImpl pnF24Client;
    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final NotificationDao notificationDao;
    private final CheckAuthComponent checkAuthComponent;
    private final NotificationViewedProducer notificationViewedProducer;
    private final MVPParameterConsumer mvpParameterConsumer;

    public NotificationAttachmentService(PnSafeStorageClientImpl safeStorageClient, PnF24ClientImpl pnF24Client, PnDeliveryPushClientImpl pnDeliveryPushClient, NotificationDao notificationDao, CheckAuthComponent checkAuthComponent, NotificationViewedProducer notificationViewedProducer, MVPParameterConsumer mvpParameterConsumer) {
        this.safeStorageClient = safeStorageClient;
        this.pnF24Client = pnF24Client;
        this.pnDeliveryPushClient = pnDeliveryPushClient;
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
        }).toList();
    }

    public static class FileDownloadIdentify {
        private Integer documentIdx;
        private Integer recipientIdx;
        private String attachmentName;
        private Integer attachmentIdx;

        private FileDownloadIdentify() {}

        public FileDownloadIdentify(Integer documentIdx) {
            this.documentIdx = documentIdx;
        }

        public FileDownloadIdentify(Integer recipientIdx, String attachmentName, Integer attachmentIdx) {
            this.recipientIdx = recipientIdx;
            this.attachmentName = attachmentName;
            this.attachmentIdx = attachmentIdx;
        }

        public static FileDownloadIdentify create(Integer documentIndex, Integer recipientIdx, String attachmentName, Integer attachmentIdx) {
            if (documentIndex != null) {
                return new FileDownloadIdentify( documentIndex );
            } else {
                return new FileDownloadIdentify( recipientIdx, attachmentName, attachmentIdx );
            }
        }
    }

    @Data
    public static class FileInfos {
        private final String fileName;
        private final FileDownloadResponse fileDownloadResponse;
        private final String fileKey;

        public FileInfos(String fileName, FileDownloadResponse fileDownloadResponse, String fileKey) {
            this.fileName = fileName;
            this.fileDownloadResponse = fileDownloadResponse;
            this.fileKey = fileKey;
        }
    }

    public NotificationAttachmentDownloadMetadataResponse downloadDocumentWithRedirect(
            String iun,
            InternalAuthHeader internalAuthHeader,
            String mandateId,
            Integer documentIdx,
            Boolean markNotificationAsViewed) {

        return downloadDocumentWithRedirectWithFileKey( iun, internalAuthHeader, mandateId,
                documentIdx, markNotificationAsViewed ).downloadMetadataResponse;
    }

    public InternalAttachmentWithFileKey downloadDocumentWithRedirectWithFileKey(
            String iun,
            InternalAuthHeader internalAuthHeader,
            String mandateId,
            Integer documentIdx,
            Boolean markNotificationAsViewed) {

        InputDownloadDto inputDownloadDto = new InputDownloadDto().toBuilder()
                .cxType(internalAuthHeader.cxType())
                .cxId(internalAuthHeader.xPagopaPnCxId())
                .uid(internalAuthHeader.xPagopaPnUid())
                .mandateId( mandateId )
                .iun( iun )
                .documentIndex( documentIdx )
                .recipientIdx( null )
                .attachmentName( null )
                .markNotificationAsViewed( markNotificationAsViewed )
                .build();
        return downloadDocumentWithRedirect( inputDownloadDto );
    }

    public NotificationAttachmentDownloadMetadataResponse downloadAttachmentWithRedirect(
            String iun,
            InternalAuthHeader internalAuthHeader,
            String mandateId,
            Integer recipientIdx,
            String attachmentName,
            Boolean markNotificationAsViewed) {

        return downloadAttachmentWithRedirectWithFileKey( iun,internalAuthHeader,mandateId,
                recipientIdx,attachmentName,markNotificationAsViewed ).downloadMetadataResponse;
    }

    public InternalAttachmentWithFileKey downloadAttachmentWithRedirectWithFileKey(
            String iun,
            InternalAuthHeader internalAuthHeader,
            String mandateId,
            Integer recipientIdx,
            String attachmentName,
            Boolean markNotificationAsViewed) {
        InputDownloadDto inputDownloadDto = new InputDownloadDto().toBuilder()
                .cxType(internalAuthHeader.cxType())
                .cxId(internalAuthHeader.xPagopaPnCxId())
                .uid(internalAuthHeader.xPagopaPnUid())
                .mandateId( mandateId )
                .iun( iun )
                .documentIndex( null )
                .recipientIdx( recipientIdx )
                .attachmentName( attachmentName )
                .markNotificationAsViewed( markNotificationAsViewed )
                .build();
        return downloadDocumentWithRedirect( inputDownloadDto );
    }


    public InternalAttachmentWithFileKey downloadAttachmentWithRedirectWithFileKey(
            String iun,
            InternalAuthHeader internalAuthHeader,
            String mandateId,
            Integer recipientIdx,
            String attachmentName,
            Integer attachmentIndex,
            Boolean markNotificationAsViewed) {
        InputDownloadDto inputDownloadDto = new InputDownloadDto().toBuilder()
                .cxType(internalAuthHeader.cxType())
                .cxId(internalAuthHeader.xPagopaPnCxId())
                .uid(internalAuthHeader.xPagopaPnUid())
                .mandateId( mandateId )
                .iun( iun )
                .documentIndex( null )
                .recipientIdx( recipientIdx )
                .attachmentName( attachmentName )
                .attachmentIdx(attachmentIndex)
                .markNotificationAsViewed( markNotificationAsViewed )
                .build();
        return downloadDocumentWithRedirect( inputDownloadDto );
    }

    private InternalAttachmentWithFileKey downloadDocumentWithRedirect(InputDownloadDto inputDownloadDto ) {
        String cxType = inputDownloadDto.getCxType();
        String cxId = inputDownloadDto.getCxId();
        String uid = inputDownloadDto.getUid();
        String mandateId = inputDownloadDto.getMandateId();
        String iun = inputDownloadDto.getIun();
        Integer recipientIdx = inputDownloadDto.getRecipientIdx();
        Integer attachmentIdx = inputDownloadDto.getAttachmentIdx();
        List<String> cxGroups = inputDownloadDto.getCxGroups();
        Integer documentIndex = inputDownloadDto.getDocumentIndex();
        String attachmentName = inputDownloadDto.getAttachmentName();
        Boolean markNotificationAsViewed = inputDownloadDto.getMarkNotificationAsViewed();
        log.info("downloadDocumentWithRedirect for cxType={} iun={} documentIndex={} recipientIdx={} xPagopaPnCxId={} attachmentName={} mandateId={} markNotificationAsViewed={}", cxType, iun, documentIndex, recipientIdx, cxId, attachmentName, mandateId, markNotificationAsViewed );

        ReadAccessAuth readAccessAuth = ReadAccessAuth.newAccessRequest( cxType, cxId, mandateId, cxGroups, iun, recipientIdx );

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
            FileDownloadIdentify fileDownloadIdentify = FileDownloadIdentify.create( documentIndex, downloadRecipientIdx, attachmentName, attachmentIdx );

            FileInfos fileInfos = computeFileInfo( fileDownloadIdentify, notification );

            // controlli per essere certi che la richiesta è stata fatta da un destinatario o da un delegato
            // ma non da rete RADD e non da mittente
            if( !cxType.equals( CxTypeAuthFleet.PA.getValue() ) && Boolean.TRUE.equals(markNotificationAsViewed) ) {
                NotificationViewDelegateInfo delegateInfo = null;
                if (StringUtils.hasText( mandateId ) ) {
                    delegateInfo = NotificationViewDelegateInfo.builder()
                            .delegateType( NotificationViewDelegateInfo.DelegateType.valueOf( cxType ) )
                            .mandateId( mandateId )
                            .internalId( cxId )
                            .operatorUuid( uid )
                            .build();
                }
                notificationViewedProducer.sendNotificationViewed( iun, Instant.now(), authorizationOutcome.getEffectiveRecipientIdx(), delegateInfo );
            }

            return InternalAttachmentWithFileKey.of(NotificationAttachmentDownloadMetadataResponse.builder()
                    .filename( fileInfos.fileName)
                    .url( fileInfos.fileDownloadResponse.getDownload().getUrl() )
                    .contentLength( nullSafeBigDecimalToInteger(
                            fileInfos.fileDownloadResponse.getContentLength()
                    ))
                    .contentType( fileInfos.fileDownloadResponse.getContentType() )
                    .sha256( fileInfos.fileDownloadResponse.getChecksum() )
                    .retryAfter( nullSafeBigDecimalToInteger(
                            fileInfos.fileDownloadResponse.getDownload().getRetryAfter()
                    )).build() , fileInfos.fileKey);
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
        String fileKey;
        String name;

        String iun = notification.getIun();
        Integer documentIndex = fileDownloadIdentify.documentIdx;
        Integer recipientIdx = fileDownloadIdentify.recipientIdx;
        Integer attachmentIdx = fileDownloadIdentify.attachmentIdx != null ? fileDownloadIdentify.attachmentIdx : 0;
        String attachmentName = fileDownloadIdentify.attachmentName;

        if(documentIndex != null){
            NotificationDocument doc = notification.getDocuments().get(documentIndex);
            name = doc.getTitle();
            fileKey = doc.getRef().getKey();
        }
        else{
            NotificationRecipient effectiveRecipient = checkRecipientsAndPayments(notification, recipientIdx, attachmentIdx);

            if (StringUtils.hasText(attachmentName) && attachmentName.equals(ATTACHMENT_TYPE_F24)) {
                List<String> pathTokens;
                NotificationPaymentInfo notificationPaymentInfo = effectiveRecipient.getPayments().get(attachmentIdx);
                if (notificationPaymentInfo.getF24() != null) {
                    pathTokens = Collections.singletonList(String.format("%d,%d", recipientIdx, attachmentIdx));
                } else {
                    pathTokens = Collections.emptyList();
                }
                return callPNF24(recipientIdx, pathTokens, notification, notificationPaymentInfo.getF24().isApplyCost());
            }
            else{
                fileKey = getFileKeyOfAttachment(iun, effectiveRecipient, attachmentName, attachmentIdx, mvpParameterConsumer.isMvp(notification.getSenderTaxId()));
                if (!StringUtils.hasText(fileKey)) {
                    String exMessage = String.format("Unable to find key for attachment=%s iun=%s with this paymentInfo=%s", attachmentName, iun, effectiveRecipient.getPayments().toString());
                    throw new PnNotFoundException("FileInfo not found", exMessage, ERROR_CODE_DELIVERY_FILEINFONOTFOUND);
                }
                name = attachmentName;
            }
        }
        MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, fileKey);
        log.info("downloadDocumentWithRedirect with fileKey={} name:{} - iun={}", fileKey, name, iun);
        return downloadFileAndBuildInfo(fileKey, iun, name);
    }

    private NotificationRecipient checkRecipientsAndPayments(InternalNotification notification, Integer recipientIdx, Integer attachmentIdx){
        if(notification.getRecipients().size()<=recipientIdx){
            String exMessage =  String.format("Notification without recipients attachment index - iun=%s", notification.getIun());
            throw new PnInternalException(exMessage, ERROR_CODE_DELIVERY_NOTIFICATIONWITHOUTRECIPIENTSATTACHMENT);
        }
        NotificationRecipient effectiveRecipient = notification.getRecipients().get(recipientIdx);
        if(effectiveRecipient.getPayments().size()<=attachmentIdx){
            String exMessage =  String.format("Notification without payment attachment index - iun=%s", notification.getIun());
            throw new PnInternalException(exMessage, ERROR_CODE_DELIVERY_NOTIFICATIONWITHOUTPAYMENTATTACHMENT);
        }
        return effectiveRecipient;
    }

    private FileInfos downloadFileAndBuildInfo(String fileKey, String iun, String name) {
        try {
            FileDownloadResponse r = this.getFile(fileKey);
            String fileName = buildFilename(iun, name, r.getContentType());

            log.info("downloadDocumentWithRedirect with fileKey={} filename:{} - iun={}", fileKey, fileName, iun);
            return new FileInfos(fileName, r, fileKey);
        } catch (Exception exc) {
            if (exc instanceof PnHttpResponseException pnHttpResponseException && pnHttpResponseException.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new PnBadRequestException("File info not found", pnHttpResponseException.getMessage(), ERROR_CODE_DELIVERY_FILEINFONOTFOUND, pnHttpResponseException);
            }
            throw exc;
        }
    }



    private FileInfos callPNF24(Integer recipientIdx, List<String> pathTokens, InternalNotification notification, boolean applyCost){
        NotificationProcessCostResponse cost = pnDeliveryPushClient.getNotificationProcessCost(notification.getIun(), recipientIdx, notification.getNotificationFeePolicy() != null ? NotificationFeePolicy.valueOf(notification.getNotificationFeePolicy().getValue()) : null, applyCost);

        F24Response f24Response = pnF24Client.generatePDF("PN-DELIVERY", notification.getIun(), pathTokens, cost.getAmount());
        FileDownloadResponse fileDownloadResponse = new FileDownloadResponse();
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo();
        fileDownloadInfo.setUrl(f24Response.getUrl());
        fileDownloadInfo.setRetryAfter(f24Response.getRetryAfter());
        fileDownloadResponse.setDownload(fileDownloadInfo);
        return new FileInfos(null, fileDownloadResponse, null);
    }

    private String getFileKeyOfAttachment(String iun, NotificationRecipient doc, String attachmentName, Integer attachmentIdx, boolean isMVPTria){
        NotificationPaymentInfo payment = doc.getPayments().get(attachmentIdx);
        if ( !Objects.nonNull( payment ) ) {
            String exMessage =  String.format("Notification without payment attachment - iun=%s", iun);
            log.error(exMessage);
            if(isMVPTria) {
                throw new PnInternalException(exMessage, ERROR_CODE_DELIVERY_NOTIFICATIONWITHOUTPAYMENTATTACHMENT);
            } else {
                throw new PnNotFoundException("FileInfo not found", exMessage, ERROR_CODE_DELIVERY_FILEINFONOTFOUND);
            }

        }
        if (attachmentName.equals(ATTACHMENT_TYPE_PAGO_PA)) {
            return getKey( payment.getPagoPa() );
        }
        return null;
    }

    private String getKey(PagoPaPayment payment) {
        String key = null;
        if (Objects.nonNull( payment ) ) {
            key = payment.getAttachment().getRef().getKey();
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


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class InternalAttachmentWithFileKey {
        NotificationAttachmentDownloadMetadataResponse downloadMetadataResponse;
        String fileKey;

        public static InternalAttachmentWithFileKey of(NotificationAttachmentDownloadMetadataResponse notification, String fileKey) {
            return new InternalAttachmentWithFileKey(notification, fileKey);
        }
    }
}
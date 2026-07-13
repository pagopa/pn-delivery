package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadInformalNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.SenderContacts;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.svc.*;
import it.pagopa.pn.delivery.svc.search.NotificationSearchService;
import it.pagopa.pn.delivery.utils.InternalFieldsCleaner;
import it.pagopa.pn.delivery.utils.LegalNotificationStatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.*;

@Slf4j
@RestController
public class PnReceivedNotificationsController implements RecipientReadApi, RecipientReadInformalNotificationApi {
    private final NotificationSearchService retrieveSvc;
    private final InformalNotificationDetailRetrieverStrategy informalNotificationDetailRetrieverStrategy;
    private final LegalNotificationDetailRetrieverStrategy legalNotificationDetailRetrieverStrategy;
    private final NotificationAttachmentService notificationAttachmentService;
    private final NotificationQRService notificationQRService;
    private final SenderContactsService senderContactsService;
    private final ModelMapper modelMapper;


    public PnReceivedNotificationsController(NotificationSearchService retrieveSvc,
                                             InformalNotificationDetailRetrieverStrategy informalNotificationDetailRetrieverStrategy,
                                             LegalNotificationDetailRetrieverStrategy legalNotificationDetailRetrieverStrategy,
                                             NotificationAttachmentService notificationAttachmentService,
                                             NotificationQRService notificationQRService, SenderContactsService senderContactsService,
                                             ModelMapper modelMapper) {
        this.retrieveSvc = retrieveSvc;
        this.informalNotificationDetailRetrieverStrategy = informalNotificationDetailRetrieverStrategy;
        this.legalNotificationDetailRetrieverStrategy = legalNotificationDetailRetrieverStrategy;
        this.notificationAttachmentService = notificationAttachmentService;
        this.notificationQRService = notificationQRService;
        this.senderContactsService = senderContactsService;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<FullNotificationSearchResponse> searchReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String mandateId, String senderId, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey, String communicationType) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_NT_SEARCH_RCP;
        String logMsg = "searchReceivedNotification";
        if (StringUtils.hasText( mandateId )) {
            eventType = PnAuditLogEventType.AUD_NT_SEARCH_DEL;
            logMsg = "searchDelegatedNotification with mandateId={}";
        }
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, logMsg, mandateId)
                .iun(iunMatch)
                .build();
        logEvent.log();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(false)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .mandateId(mandateId)
                .filterId(senderId)
                .statuses(List.of())
                .communicationType(StringUtils.hasText(communicationType) ? NotificationSearchCommunicationType.valueOf(communicationType) : NotificationSearchCommunicationType.LEGAL)
                //.groups( groups != null ? Arrays.asList( groups ) : null )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        log.info("Search received notification with filter senderId={} iun={}", senderId, iunMatch);
        ResultPaginationDto<NotificationSearchRow, String> serviceResult;
        FullNotificationSearchResponse response = new FullNotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, xPagopaPnCxType.getValue(), xPagopaPnCxGroups);
            response = modelMapper.map(serviceResult, FullNotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<LegalNotificationSearchResponse> searchReceivedDelegatedNotification(String xPagopaPnUid,
                                                                                          CxTypeAuthFleet xPagopaPnCxType,
                                                                                          String xPagopaPnCxId,
                                                                                          OffsetDateTime startDate,
                                                                                          OffsetDateTime endDate,
                                                                                          List<String> xPagopaPnCxGroups,
                                                                                          String senderId,
                                                                                          String recipientId,
                                                                                          String group,
                                                                                          String iunMatch,
                                                                                          NotificationStatusV26 status,
                                                                                          Integer size,
                                                                                          String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_RCP, "searchReceivedDelegatedNotification")
                .build();
        logEvent.log();
        InputSearchNotificationDelegatedDto searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .group(group)
                .senderId(senderId)
                .iun(iunMatch)
                .receiverId(recipientId)
                .statuses(status != null ? List.of(status) : Collections.emptyList())
                .size(size)
                .nextPageKey(nextPagesKey)
                .cxGroups(xPagopaPnCxGroups)
                .build();
        log.info("Search received delegated notification to {} with filter senderId={} recipientId={}", xPagopaPnCxId, senderId, recipientId);
        ResultPaginationDto<NotificationSearchRow, String> result;
        LegalNotificationSearchResponse response;
        try {
            result = retrieveSvc.searchNotificationDelegated(searchDto);
            LegalNotificationStatusValidator.assertLegalCompatible(result);
            response = modelMapper.map(result, LegalNotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException e) {
            log.error("can not search received delegated notification", e);
            logEvent.generateFailure("" + e.getProblem()).log();
            throw  e;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FullReceivedNotificationV28> getReceivedNotificationV28(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedNotificationV28 result = null;
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_NT_VIEW_RCP;
        String logMsg = "getReceivedNotification";
        if (StringUtils.hasText( mandateId )) {
            eventType = PnAuditLogEventType.AUD_NT_VIEW_DEL;
            logMsg = "getReceivedNotificationDelegate with mandateId={}";
        }
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, logMsg, mandateId)
                .iun(iun)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId != null ? mandateId.toString() : "null")
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups, xPagopaPnSrcCh, xPagopaPnSrcChDetails);
            LegalNotificationDetail legalNotificationDetail = legalNotificationDetailRetrieverStrategy.getNotificationAndNotifyViewedEvent(iun, internalAuthHeader, mandateId, logEvent);
            InternalNotification internalNotification = legalNotificationDetail.getNotification();
            InternalFieldsCleaner.cleanInternalFields( internalNotification );
            result = modelMapper.map(legalNotificationDetail, FullReceivedNotificationV28.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public  ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, Integer docIdx, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, UUID mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_NT_DOCOPEN_RCP;
        String logMsg = "getReceivedNotificationDocument from documents array with index={}";
        if (mandateId != null && StringUtils.hasText( mandateId.toString() )) {
            eventType = PnAuditLogEventType.AUD_NT_DOCOPEN_DEL;
            logMsg = "getDelegateNotificationDocument from documents array with index={} with mandateId={}";
        }
        NotificationAttachmentDownloadMetadataResponse response;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, logMsg, docIdx, mandateId)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            legalNotificationDetailRetrieverStrategy.checkIfNotificationIsNotCancelled(iun);
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(
                    xPagopaPnCxType.getValue(),
                    xPagopaPnCxId,
                    xPagopaPnUid,
                    xPagopaPnCxGroups,
                    xPagopaPnSrcCh,
                    xPagopaPnSrcChDetails
            );
            response = notificationAttachmentService.downloadDocumentWithRedirect(
                    iun,
                    internalAuthHeader,
                    mandateId != null ? mandateId.toString() : null,
                    docIdx,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationDocument {}", message).log();
            return ResponseEntity.ok(response);
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
    }


    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, String attachmentName, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, UUID mandateId, Integer attachmentIdx) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_NT_ATCHOPEN_RCP;
        String logMsg = "getReceivedNotificationAttachment attachment name={}, attachment index={}";
        if (mandateId != null && StringUtils.hasText( mandateId.toString() )) {
            eventType = PnAuditLogEventType.AUD_NT_ATCHOPEN_DEL;
            logMsg = "getReceivedAndDelegatedNotificationAttachment attachment name={}, attachment index={} and mandateId={}";
        }
        NotificationAttachmentDownloadMetadataResponse response;
        PnAuditLogEvent logEvent = auditLogBuilder.before(eventType, logMsg, attachmentName, attachmentIdx, mandateId)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            legalNotificationDetailRetrieverStrategy.checkIfNotificationIsNotCancelled(iun);
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(
                    xPagopaPnCxType.getValue(),
                    xPagopaPnCxId,
                    xPagopaPnUid,
                    xPagopaPnCxGroups,
                    xPagopaPnSrcCh,
                    xPagopaPnSrcChDetails
            );
            response = notificationAttachmentService.downloadAttachmentWithRedirect(
                    iun,
                    internalAuthHeader,
                    mandateId != null ? mandateId.toString() : null,
                    null,
                    attachmentName,
                    attachmentIdx,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationAttachment attachment name={} attachment index={}, {}",
                    attachmentName, attachmentIdx, message).log();
            return ResponseEntity.ok(response);
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
    }

    @Override
    public ResponseEntity<ResponseCheckAarMandateDto> checkAarQrCode(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, RequestCheckAarMandateDto requestCheckAarMandateDto, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        String aarQrCodeValue = requestCheckAarMandateDto.getAarQrCodeValue();
        String recipientType = xPagopaPnCxType.getValue();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before( PnAuditLogEventType.AUD_NT_REQQR, "getNotificationQr aarQrCodeValue={} recipientType={} customerId={}",
                        aarQrCodeValue,
                        recipientType,
                        xPagopaPnCxId)
                .mdcEntry(MDC_PN_CTX_TOPIC, String.format("aarQrCodeValue=%s", aarQrCodeValue))
                .build();
        logEvent.log();
        ResponseCheckAarMandateDto responseCheckAarMandateDto;
        try {
            responseCheckAarMandateDto = notificationQRService.getNotificationByQRWithMandate( requestCheckAarMandateDto, xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnCxGroups );
            logEvent.getMdc().put(MDC_PN_IUN_KEY, responseCheckAarMandateDto.getIun());
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification by qr= " + exc.getProblem()).log();
            throw exc;
        }

        return ResponseEntity.ok( responseCheckAarMandateDto );
    }


    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedInformalNotificationAttachmentV1(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, String attachmentName, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, Integer attachmentIdx) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_COM_ATCHOPEN_RCP;
        String logMsg = "getReceivedInformalNotificationAttachmentV1 attachment name={}, attachment index={}";
        NotificationAttachmentDownloadMetadataResponse response;
        PnAuditLogEvent logEvent = auditLogBuilder.before(eventType, logMsg, attachmentName, attachmentIdx)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(
                    xPagopaPnCxType.getValue(),
                    xPagopaPnCxId,
                    xPagopaPnUid,
                    xPagopaPnCxGroups,
                    xPagopaPnSrcCh,
                    xPagopaPnSrcChDetails
            );
            response = notificationAttachmentService.downloadAttachmentWithRedirect(
                    iun,
                    internalAuthHeader,
                    null,
                    null,
                    attachmentName,
                    attachmentIdx,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedInformalNotificationAttachmentV1 attachment name={} attachment index={}, {}",
                    attachmentName, attachmentIdx, message).log();
            return ResponseEntity.ok(response);
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedInformalNotificationDocumentV1(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, Integer docIdx, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_COM_DOCOPEN_RCP;
        String logMsg = "getReceivedInformalNotificationDocumentV1 from documents array with index={}";
        NotificationAttachmentDownloadMetadataResponse response;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, logMsg, docIdx)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(
                    xPagopaPnCxType.getValue(),
                    xPagopaPnCxId,
                    xPagopaPnUid,
                    xPagopaPnCxGroups,
                    xPagopaPnSrcCh,
                    xPagopaPnSrcChDetails
            );
            response = notificationAttachmentService.downloadDocumentWithRedirect(
                    iun,
                    internalAuthHeader,
                    null,
                    docIdx,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedInformalNotificationDocumentV1 {}", message).log();
            return ResponseEntity.ok(response);
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
    }

    @Override
    public ResponseEntity<FullReceivedInformalNotificationV1> getReceivedInformalNotificationV1(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, Boolean retrieveMessage) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedInformalNotificationV1 result = null;
        PnAuditLogEventType eventType = PnAuditLogEventType.AUD_COM_VIEW_RCP;
        String logMsg = "getReceivedInformalNotification";
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(eventType, logMsg)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups, xPagopaPnSrcCh, xPagopaPnSrcChDetails);
            InformalNotificationDetail informalNotificationDetail = informalNotificationDetailRetrieverStrategy.getNotificationAndNotifyViewedEvent(
                    iun,
                    internalAuthHeader,
                    logEvent,
                    Boolean.TRUE.equals(retrieveMessage)
            );
            InternalNotification internalNotification = informalNotificationDetail.getNotification();
            InternalFieldsCleaner.cleanInternalFields( internalNotification );
            result = modelMapper.map(informalNotificationDetail, FullReceivedInformalNotificationV1.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return RecipientReadApi.super.getRequest();
    }

    @Override
    public ResponseEntity<SenderContacts> getSenderContacts(String senderId) {
        log.info("getSenderContacts for senderId={}", senderId);
        SenderContactsDto senderContactsDto = senderContactsService.getSenderContacts(senderId);
        return ResponseEntity.ok(modelMapper.map(senderContactsDto, SenderContacts.class));
    }
}

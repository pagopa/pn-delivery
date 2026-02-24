package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.InternalFieldsCleaner;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_TOPIC;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@Slf4j
@RestController
public class PnReceivedNotificationsController implements RecipientReadApi {
    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final NotificationQRService notificationQRService;

    private final ModelMapper modelMapper;


    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, NotificationQRService notificationQRService, ModelMapper modelMapper) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.notificationQRService = notificationQRService;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<NotificationSearchResponse> searchReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String mandateId, String senderId, NotificationStatusV26 status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
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
                .statuses(status==null?List.of():List.of(status))
                //.groups( groups != null ? Arrays.asList( groups ) : null )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        log.info("Search received notification with filter senderId={} iun={}", senderId, iunMatch);
        ResultPaginationDto<NotificationSearchRow, String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, xPagopaPnCxType.getValue(), xPagopaPnCxGroups);
            response = modelMapper.map(serviceResult, NotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<NotificationSearchResponse> searchReceivedDelegatedNotification(String xPagopaPnUid,
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
        NotificationSearchResponse response;
        try {
            result = retrieveSvc.searchNotificationDelegated(searchDto);
            response = modelMapper.map(result, NotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException e) {
            log.error("can not search received delegated notification", e);
            logEvent.generateFailure("" + e.getProblem()).log();
            throw  e;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FullReceivedNotificationV27> getReceivedNotificationV27(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, String iun, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedNotificationV27 result = null;
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
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, internalAuthHeader, mandateId, logEvent);
            InternalFieldsCleaner.cleanInternalFields( internalNotification );
            result = modelMapper.map(internalNotification, FullReceivedNotificationV27.class);
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
            retrieveSvc.checkIfNotificationIsNotCancelled(iun);
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
            retrieveSvc.checkIfNotificationIsNotCancelled(iun);
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
}

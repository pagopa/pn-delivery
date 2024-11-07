package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService.InternalAttachmentWithFileKey;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.InternalFieldsCleaner;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;


@RestController
@Slf4j
public class PnSentNotificationsController implements SenderReadB2BApi,SenderReadWebApi {

    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final ModelMapper modelMapper;

    public PnSentNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, ModelMapper modelMapper) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapper = modelMapper;
    }

    @Override
    public ResponseEntity<FullSentNotificationV25> getSentNotificationV25(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups) {
        InternalNotification internalNotification = retrieveSvc.getNotificationInformationWithSenderIdCheck( iun, xPagopaPnCxId, xPagopaPnCxGroups );
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_SND, "getSenderNotification")
                .iun(iun)
                .build();
        logEvent.log();
        if ( NotificationStatus.IN_VALIDATION.equals( internalNotification.getNotificationStatus() )
                || NotificationStatus.REFUSED.equals( internalNotification.getNotificationStatus() ) ) {
            logEvent.generateFailure("Unable to find notification with iun={} cause status={}", internalNotification.getIun(), internalNotification.getNotificationStatus()).log();
            throw new PnNotificationNotFoundException( "Unable to find notification with iun="+ internalNotification.getIun() );
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        FullSentNotificationV25 result = modelMapper.map( internalNotification, FullSentNotificationV25.class );
        logEvent.generateSuccess().log();
        return ResponseEntity.ok( result );
    }



    @Override
    public ResponseEntity<NotificationSearchResponse> searchSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String recipientId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchSentNotification")
                .iun(iunMatch)
                .build();
        logEvent.log();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .bySender(true)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .statuses(status==null?List.of():List.of(status))
                .receiverIdIsOpaque(false)
                .groups( xPagopaPnCxGroups )
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, null, null);
            response = modelMapper.map( serviceResult, NotificationSearchResponse.class );
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SenderReadB2BApi.super.getRequest();
    }

    @Override
    public ResponseEntity<NewNotificationRequestStatusResponseV24> getNotificationRequestStatusV24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String notificationRequestId, String paProtocolNumber, String idempotenceToken) {
        InternalNotification internalNotification;
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before( PnAuditLogEventType.AUD_NT_CHECK, "getNotificationRequestStatus notificationRequestId={} paProtocolNumber={} idempotenceToken={}",
                        notificationRequestId,
                        paProtocolNumber,
                        idempotenceToken)
                .build();
        logEvent.log();
        if (StringUtils.hasText( notificationRequestId )) {
            String iun = new String(Base64Utils.decodeFromString(notificationRequestId), StandardCharsets.UTF_8);
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            internalNotification = retrieveSvc.getNotificationInformationWithSenderIdCheck( iun, xPagopaPnCxId, xPagopaPnCxGroups );
        } else {
            if ( !StringUtils.hasText( paProtocolNumber ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "paProtocolNumber");
                logEvent.generateFailure("[notificationRequestId={} idempotenceToken={}]" + e.getProblem(), notificationRequestId, idempotenceToken);
                throw e;
            }
            if (!StringUtils.hasText( idempotenceToken ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "idempotenceToken");
                logEvent.generateFailure("[notificationRequestId={} paProtocolNumber={}]" + e.getProblem(), notificationRequestId, paProtocolNumber);
                throw e;
            }
            internalNotification = retrieveSvc.getNotificationInformation( xPagopaPnCxId, paProtocolNumber, idempotenceToken, xPagopaPnCxGroups);
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        NewNotificationRequestStatusResponseV24 response = modelMapper.map(
                internalNotification,
                NewNotificationRequestStatusResponseV24.class
        );
        response.setNotificationRequestId( Base64Utils.encodeToString( internalNotification.getIun().getBytes(StandardCharsets.UTF_8) ));

        NotificationStatus lastStatus;
        if ( !CollectionUtils.isEmpty( internalNotification.getNotificationStatusHistory() )) {
            lastStatus = internalNotification.getNotificationStatusHistory().get(
                    internalNotification.getNotificationStatusHistory().size() - 1 ).getStatus();
        } else {
            log.debug( "No status history for notificationRequestId={}", notificationRequestId );
            lastStatus = NotificationStatus.IN_VALIDATION;
        }

        switch (lastStatus) {
            case IN_VALIDATION -> {
                response.setNotificationRequestStatus("WAITING");
                response.retryAfter(10);
                response.setIun(null);
            }
            case REFUSED -> {
                response.setNotificationRequestStatus("REFUSED");
                response.setIun(null);
                Optional<TimelineElementV25> timelineElement = internalNotification.getTimeline().stream().filter(
                        tle -> TimelineElementCategoryV23.REQUEST_REFUSED.equals(tle.getCategory())).findFirst();
                timelineElement.ifPresent(element -> setRefusedErrors(response, element));
            }
            default -> response.setNotificationRequestStatus("ACCEPTED");
        }

        logEvent.generateSuccess().log();
        return ResponseEntity.ok( response );
    }

    private void setRefusedErrors(NewNotificationRequestStatusResponseV24 response, TimelineElementV25 timelineElement) {
        List<NotificationRefusedErrorV25> refusalReasons = timelineElement.getDetails().getRefusalReasons();
        List<ProblemError> problemErrorList = refusalReasons.stream().map(
                reason -> ProblemError.builder()
                        .code( reason.getErrorCode() )
                        .detail( reason.getDetail() )
                        .build()
        ).toList();
        response.setErrors( problemErrorList );
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer recipientIdx, String attachmentName, List<String> xPagopaPnCxGroups, Integer attachmentIdx) {
        InternalAttachmentWithFileKey internalAttachmentWithFileKey = new InternalAttachmentWithFileKey();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_ATCHOPEN_SND, "getSentNotificationAttachment attachment name={} attachment index={}", attachmentName, attachmentIdx)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups);
            internalAttachmentWithFileKey = notificationAttachmentService.downloadAttachmentWithRedirectWithFileKey(
                    iun,
                    internalAuthHeader,
                    null,
                    recipientIdx,
                    attachmentName,
                    attachmentIdx,
                    false
            );
            if(internalAttachmentWithFileKey == null || internalAttachmentWithFileKey.getFileKey() == null){
                logEvent.generateSuccess().log();
            }else{
                logEvent.getMdc().put(MDC_PN_CTX_SAFESTORAGE_FILEKEY, internalAttachmentWithFileKey.getFileKey());
                logEvent.generateSuccess().log();
            }
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
     
        return ResponseEntity.ok( internalAttachmentWithFileKey == null ? null : internalAttachmentWithFileKey.getDownloadMetadataResponse() );
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer docIdx, List<String> xPagopaPnCxGroups) {
        InternalAttachmentWithFileKey internalAttachmentWithFileKey = new InternalAttachmentWithFileKey();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_SND, "getSentNotificationDocument={}", docIdx)
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnUid, xPagopaPnCxGroups);
            internalAttachmentWithFileKey = notificationAttachmentService.downloadDocumentWithRedirectWithFileKey(
                    iun,
                    internalAuthHeader,
                    null,
                    docIdx,
                    false
            );
            if(internalAttachmentWithFileKey == null || internalAttachmentWithFileKey.getFileKey() == null){
                logEvent.generateSuccess().log();
            }else{
                logEvent.getMdc().put(MDC_PN_CTX_SAFESTORAGE_FILEKEY, internalAttachmentWithFileKey.getFileKey());
                logEvent.generateSuccess().log();
            }
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( internalAttachmentWithFileKey == null ? null : internalAttachmentWithFileKey.getDownloadMetadataResponse() );
    }
}

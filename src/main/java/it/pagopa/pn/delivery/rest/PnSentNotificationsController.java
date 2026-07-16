package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InformalNotificationTerminationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadInformalNotificationB2BApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.models.*;
import it.pagopa.pn.delivery.svc.InformalNotificationDetailRetrieverStrategy;
import it.pagopa.pn.delivery.svc.LegalNotificationDetailRetrieverStrategy;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService.InternalAttachmentWithFileKey;
import it.pagopa.pn.delivery.svc.NotificationDetailRetrieverStrategy;
import it.pagopa.pn.delivery.svc.search.NotificationSearchService;
import it.pagopa.pn.delivery.utils.InternalFieldsCleaner;
import it.pagopa.pn.delivery.utils.LegalNotificationStatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
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
import static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26.REFUSED;
import static it.pagopa.pn.delivery.utils.NotificationUtils.*;


@RestController
@Slf4j
public class PnSentNotificationsController implements SenderReadB2BApi, SenderReadWebApi, SenderReadInformalNotificationB2BApi, InformalNotificationTerminationApi {

    private final NotificationSearchService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final ModelMapper modelMapper;
    private final LegalNotificationDetailRetrieverStrategy legalNotificationDetailRetrieverStrategy;
    private final InformalNotificationDetailRetrieverStrategy informalNotificationDetailRetrieverStrategy;

    public PnSentNotificationsController(NotificationSearchService retrieveSvc,
                                         NotificationAttachmentService notificationAttachmentService,
                                         ModelMapper modelMapper,
                                         LegalNotificationDetailRetrieverStrategy legalNotificationDetailRetrieverStrategy,
                                         InformalNotificationDetailRetrieverStrategy informalNotificationDetailRetrieverStrategy) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapper = modelMapper;
        this.legalNotificationDetailRetrieverStrategy = legalNotificationDetailRetrieverStrategy;
        this.informalNotificationDetailRetrieverStrategy = informalNotificationDetailRetrieverStrategy;
    }

    @Override
    public ResponseEntity<FullSentNotificationV29> getSentNotificationV29(String xPagopaPnUid,
                                                                          CxTypeAuthFleet xPagopaPnCxType,
                                                                          String xPagopaPnCxId,
                                                                          String iun,
                                                                          List<String> xPagopaPnCxGroups) {
        LegalNotificationDetail legalNotificationDetail = legalNotificationDetailRetrieverStrategy.getNotificationInformationWithSenderIdCheck( iun, xPagopaPnCxId, xPagopaPnCxGroups );
        InternalNotification internalNotification = legalNotificationDetail.getNotification();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_SND, "getSenderNotification")
                .iun(iun)
                .build();
        logEvent.log();
        if ( NotificationStatusV26.IN_VALIDATION.equals( legalNotificationDetail.getNotificationStatus() )
                || REFUSED.equals( legalNotificationDetail.getNotificationStatus() ) ) {
            logEvent.generateFailure("Unable to find notification with iun={} cause status={}", internalNotification.getIun(),
                    legalNotificationDetail.getNotificationStatus()).log();
            throw new PnNotificationNotFoundException( "Unable to find notification with iun="+ internalNotification.getIun() );
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        FullSentNotificationV29 result = modelMapper.map( legalNotificationDetail, FullSentNotificationV29.class );
        logEvent.generateSuccess().log();
        return ResponseEntity.ok( result );
    }



    @Override
    public ResponseEntity<LegalNotificationSearchResponse> searchSentNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String recipientId, NotificationStatusV26 status, String iunMatch, Integer size, String nextPagesKey) {
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
                .iunMatch(iunMatch)
                // la ricerca lato mittente è esclusivamente legale: si forza esplicitamente il filtro così da escludere le comunicazioni bonarie
                .communicationType(NotificationSearchCommunicationType.LEGAL)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        LegalNotificationSearchResponse response = new LegalNotificationSearchResponse();
        try {
            serviceResult = retrieveSvc.searchNotification(searchDto, null, null);
            LegalNotificationStatusValidator.assertLegalCompatible( serviceResult );
            response = modelMapper.map( serviceResult, LegalNotificationSearchResponse.class );
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
    public ResponseEntity<NewNotificationRequestStatusResponseV26> getNotificationRequestStatusV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String notificationRequestId, String paProtocolNumber, String idempotenceToken) {
        LegalNotificationDetail legalNotificationDetail;
        InternalNotification internalNotification;
        PnAuditLogEvent logEvent = buildLogEventForRequestStatus(PnAuditLogEventType.AUD_NT_CHECK, "getNotificationRequestStatus", notificationRequestId, paProtocolNumber, idempotenceToken);

        logEvent.log();
        legalNotificationDetail = (LegalNotificationDetail) retrieveNotificationForRequestStatus(notificationRequestId, paProtocolNumber, idempotenceToken, xPagopaPnCxId, xPagopaPnCxGroups, logEvent, legalNotificationDetailRetrieverStrategy);
        internalNotification = legalNotificationDetail.getNotification();
        NewNotificationRequestStatusResponseV26 response = modelMapper.map(
                internalNotification,
                NewNotificationRequestStatusResponseV26.class
        );
        response.setNotificationRequestId( Base64Utils.encodeToString( internalNotification.getIun().getBytes(StandardCharsets.UTF_8) ));

        NotificationStatusV26 lastStatus = getNotificationLastStatus(notificationRequestId, legalNotificationDetail);

        switch (lastStatus) {
            case IN_VALIDATION -> {
                response.setNotificationRequestStatus("WAITING");
                response.retryAfter(10);
                response.setIun(null);
            }
            case REFUSED -> {
                response.setNotificationRequestStatus("REFUSED");
                response.setIun(null);
                Optional<TimelineElementV28> timelineElement = legalNotificationDetail.getTimeline().stream().filter(
                        tle -> TimelineElementCategoryV28.REQUEST_REFUSED.equals(tle.getCategory())).findFirst();
                timelineElement.ifPresent(element -> response.setErrors(getLegalNotificationRefusedErrors(element)));
            }
            default -> response.setNotificationRequestStatus("ACCEPTED");
        }

        logEvent.generateSuccess().log();
        return ResponseEntity.ok( response );
    }

    private PnAuditLogEvent buildLogEventForRequestStatus(PnAuditLogEventType pnAuditLogEventType, String methodName, String notificationRequestId, String paProtocolNumber, String idempotenceToken) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before( pnAuditLogEventType, "{} notificationRequestId={} paProtocolNumber={} idempotenceToken={}",
                        methodName,
                        notificationRequestId,
                        paProtocolNumber,
                        idempotenceToken)
                .build();
    }

    private NotificationDetail retrieveNotificationForRequestStatus(
            String notificationRequestId,
            String paProtocolNumber,
            String idempotenceToken,
            String xPagopaPnCxId,
            List<String> xPagopaPnCxGroups,
            PnAuditLogEvent logEvent,
            NotificationDetailRetrieverStrategy<?> retrieverStrategy
    ) {
        NotificationDetail notificationDetail;
        InternalNotification internalNotification;
        if (StringUtils.hasText( notificationRequestId )) {
            String iun = new String(Base64Utils.decodeFromString(notificationRequestId), StandardCharsets.UTF_8);
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            notificationDetail = retrieverStrategy.getNotificationInformationWithSenderIdCheck( iun, xPagopaPnCxId, xPagopaPnCxGroups );
        } else {
            if ( !StringUtils.hasText( paProtocolNumber ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "paProtocolNumber");
                logEvent.generateFailure("[notificationRequestId={} idempotenceToken={}]" + e.getProblem(), notificationRequestId, idempotenceToken).log();
                throw e;
            }
            if (!StringUtils.hasText( idempotenceToken ) ) {
                PnInvalidInputException e = new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, "idempotenceToken");
                logEvent.generateFailure("[notificationRequestId={} paProtocolNumber={}]" + e.getProblem(), notificationRequestId, paProtocolNumber).log();
                throw e;
            }
            notificationDetail = retrieverStrategy.getNotificationInformation(xPagopaPnCxId, paProtocolNumber, idempotenceToken, xPagopaPnCxGroups);
        }
        internalNotification = notificationDetail.getNotification();
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        return notificationDetail;
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer recipientIdx, String attachmentName, List<String> xPagopaPnCxGroups, Integer attachmentIdx) {
        AttachmentDownloadRequest attachmentDownloadRequest = new AttachmentDownloadRequest(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, xPagopaPnCxGroups, iun, recipientIdx, attachmentName, attachmentIdx);
        LogConfig logConfig = new LogConfig(PnAuditLogEventType.AUD_NT_ATCHOPEN_SND, "getSentNotificationAttachment attachment name={} attachment index={}");
        return getInternalNotificationAttachment(attachmentDownloadRequest, logConfig);

    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer docIdx, List<String> xPagopaPnCxGroups) {
        DocumentDownloadRequest request = new DocumentDownloadRequest(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, iun, docIdx, xPagopaPnCxGroups);
        LogConfig logConfig = new LogConfig(PnAuditLogEventType.AUD_NT_DOCOPEN_SND, "getSentNotificationDocument={}");
        return getNotificationDocumentInternal(request, logConfig);
    }

    @Override
    public ResponseEntity<NewInformalNotificationRequestStatusResponseV1> getInformalNotificationRequestStatusV1(String xPagopaPnUid,
                                                                                                                 CxTypeAuthFleet xPagopaPnCxType,
                                                                                                                 String xPagopaPnCxId,
                                                                                                                 List<String> xPagopaPnCxGroups,
                                                                                                                 String notificationRequestId,
                                                                                                                 String paProtocolNumber,
                                                                                                                 String idempotenceToken) {
        InformalNotificationDetail informalNotificationDetail;
        InternalNotification internalNotification;
        PnAuditLogEvent logEvent = buildLogEventForRequestStatus(PnAuditLogEventType.AUD_COM_CHECK, "getInformalNotificationRequestStatusV1", notificationRequestId, paProtocolNumber, idempotenceToken);
        logEvent.log();

        informalNotificationDetail = (InformalNotificationDetail) retrieveNotificationForRequestStatus(notificationRequestId, paProtocolNumber, idempotenceToken, xPagopaPnCxId, xPagopaPnCxGroups, logEvent, informalNotificationDetailRetrieverStrategy);
        internalNotification = informalNotificationDetail.getNotification();
        NewInformalNotificationRequestStatusResponseV1 response = modelMapper.map(
                internalNotification,
                NewInformalNotificationRequestStatusResponseV1.class
        );
        response.setNotificationRequestId( Base64Utils.encodeToString( internalNotification.getIun().getBytes(StandardCharsets.UTF_8) ));

        InformalNotificationStatusV1 lastStatus = getInformalNotificationLastStatus(notificationRequestId, informalNotificationDetail);

        switch (lastStatus) {
            case IN_VALIDATION -> {
                response.setNotificationRequestStatus("WAITING");
                response.retryAfter(10);
                response.setIun(null);
            }
            case REFUSED -> {
                response.setNotificationRequestStatus("REFUSED");
                response.setIun(null);
                Optional<InformalTimelineElementV1> timelineElement = informalNotificationDetail.getTimeline().stream().filter(
                        tle -> InformalTimelineElementCategoryV1.REQUEST_REFUSED.equals(tle.getCategory())).findFirst();
                timelineElement.ifPresent(element -> response.setErrors(getInformalNotificationRefusedErrors(element)));
            }
            default -> response.setNotificationRequestStatus("ACCEPTED");
        }

        logEvent.generateSuccess().log();
        return ResponseEntity.ok( response );
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentInformalNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer recipientIdx, String attachmentName, List<String> xPagopaPnCxGroups, Integer attachmentIdx) {
        AttachmentDownloadRequest attachmentDownloadRequest = new AttachmentDownloadRequest(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, xPagopaPnCxGroups, iun, recipientIdx, attachmentName, attachmentIdx);
        LogConfig logConfig = new LogConfig(PnAuditLogEventType.AUD_COM_ATCHOPEN_SND, "getSentInformalNotificationAttachment attachment name={} attachment index={}");
        return getInternalNotificationAttachment(attachmentDownloadRequest, logConfig);
    }

    @Override
    public ResponseEntity<TerminationRequestStatus> terminateInformalWorkflow(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getSentInformalNotificationDocument(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            String iun,
            Integer docIdx,
            List<String> xPagopaPnCxGroups
    ) {
        DocumentDownloadRequest request = new DocumentDownloadRequest(xPagopaPnUid, xPagopaPnCxType, xPagopaPnCxId, iun, docIdx, xPagopaPnCxGroups);
        LogConfig logConfig = new LogConfig(PnAuditLogEventType.AUD_COM_DOCOPEN_SND, "getSentInformalNotificationDocument docIdx={}");
        return getNotificationDocumentInternal(request, logConfig);
    }

    @Override
    public ResponseEntity<FullSentInformalNotificationV1> getSentInformalNotificationV1(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups, Boolean retrieveMessage) {
        InformalNotificationDetail informalNotificationDetail =
                informalNotificationDetailRetrieverStrategy.getNotificationInformationWithSenderIdCheck(iun, xPagopaPnCxId, xPagopaPnCxGroups, Boolean.TRUE.equals(retrieveMessage));
        InternalNotification internalNotification = informalNotificationDetail.getNotification();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_COM_VIEW_SND, "getSenderInformalNotification")
                .iun(iun)
                .build();
        logEvent.log();
        if ( InformalNotificationStatusV1.IN_VALIDATION.equals( informalNotificationDetail.getNotificationStatus() )
                || InformalNotificationStatusV1.REFUSED.equals( informalNotificationDetail.getNotificationStatus() ) ) {
            logEvent.generateFailure("Unable to find informal notification with iun={} cause status={}", internalNotification.getIun(), informalNotificationDetail.getNotificationStatus()).log();
            throw new PnNotificationNotFoundException( "Unable to find informal notification with iun="+ internalNotification.getIun() );
        }
        InternalFieldsCleaner.cleanInternalFields( internalNotification );
        FullSentInformalNotificationV1 result = modelMapper.map( informalNotificationDetail,
                FullSentInformalNotificationV1.class );
        logEvent.generateSuccess().log();
        return ResponseEntity.ok( result );
    }

    /**
     * Metodo privato per gestire la logica comune di download documento con log parametrizzabile e parametri raggruppati
     */
    private ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getNotificationDocumentInternal(
            DocumentDownloadRequest request,
            LogConfig logConfig
    ) {
        InternalAttachmentWithFileKey internalAttachmentWithFileKey = new InternalAttachmentWithFileKey();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(logConfig.logEventType, logConfig.logMsg, request.docIdx)
                .iun(request.iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(request.xPagopaPnCxType.getValue(), request.xPagopaPnCxId, request.xPagopaPnUid, request.xPagopaPnCxGroups);
            internalAttachmentWithFileKey = notificationAttachmentService.downloadDocumentWithRedirectWithFileKey(
                    request.iun,
                    internalAuthHeader,
                    null,
                    request.docIdx,
                    false
            );
            if (internalAttachmentWithFileKey == null || internalAttachmentWithFileKey.getFileKey() == null) {
                logEvent.generateSuccess().log();
            } else {
                logEvent.getMdc().put(MDC_PN_CTX_SAFESTORAGE_FILEKEY, internalAttachmentWithFileKey.getFileKey());
                logEvent.generateSuccess().log();
            }
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(internalAttachmentWithFileKey == null ? null : internalAttachmentWithFileKey.getDownloadMetadataResponse());
    }

    /**
         * DTO per raggruppare i parametri della richiesta di download documento
         */
        private record DocumentDownloadRequest(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId,
                                               String iun, Integer docIdx, List<String> xPagopaPnCxGroups) {
    }

    /**
         * DTO per raggruppare i parametri di logging
         */
        private record LogConfig(PnAuditLogEventType logEventType, String logMsg) {
    }

    private ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getInternalNotificationAttachment(
            AttachmentDownloadRequest request,
            LogConfig logConfig
    ) {
        InternalAttachmentWithFileKey internalAttachmentWithFileKey;
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(logConfig.logEventType, logConfig.logMsg, request.attachmentName, request.attachmentIdx)
                .iun(request.iun)
                .build();
        logEvent.log();
        try {
            InternalAuthHeader internalAuthHeader = new InternalAuthHeader(request.xPagopaPnCxType.getValue(), request.xPagopaPnCxId, request.xPagopaPnUid, request.xPagopaPnCxGroups);
            internalAttachmentWithFileKey = notificationAttachmentService.downloadAttachmentWithRedirectWithFileKey(
                    request.iun,
                    internalAuthHeader,
                    null,
                    request.recipientIdx,
                    request.attachmentName,
                    request.attachmentIdx,
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

    private record AttachmentDownloadRequest(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups,
                                           String iun, Integer recipientIdx, String attachmentName, Integer attachmentIdx) {
    }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestController
public class PnReceivedNotificationsController implements RecipientReadApi {
    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;

    private final ModelMapperFactory modelMapperFactory;

    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<NotificationSearchResponse> searchReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String mandateId, String senderId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_RCP, "searchReceivedNotification")
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .iun(iunMatch)
                .uid(xPagopaPnUid)
                .build();
        logEvent.log();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
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
            serviceResult = retrieveSvc.searchNotification(searchDto);
            ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class);
            response = mapper.map(serviceResult, NotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (Exception exc ){
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FullReceivedNotification> getReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedNotification result = null;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RPC, "getReceivedNotification")
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .iun(iun)
                .uid(xPagopaPnUid)
                .build();
        logEvent.log();
        try {
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, xPagopaPnCxId, mandateId);

            ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, FullReceivedNotification.class);

            result = mapper.map(internalNotification, FullReceivedNotification.class);

            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer docIdx, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_RCP, "getReceivedNotificationDocument {}", docIdx)
                .uid(xPagopaPnUid)
                .iun(iun)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .build();
        logEvent.log();
        try {
            response = notificationAttachmentService.downloadDocumentWithRedirect(
                    iun,
                    xPagopaPnCxType.toString(),
                    xPagopaPnCxId,
                    mandateId,
                    docIdx,
                    false
            );
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, String attachmentName, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_ATCHOPEN_RCP, "getReceivedNotificationAttachment={}", attachmentName)
                .iun(iun)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .uid(xPagopaPnUid)
                .build();
        logEvent.log();
        try {
            response = notificationAttachmentService.downloadAttachmentWithRedirect(
                    iun,
                    xPagopaPnCxType.toString(),
                    xPagopaPnCxId,
                    mandateId,
                    null,
                    attachmentName,
                    false
            );
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }
}

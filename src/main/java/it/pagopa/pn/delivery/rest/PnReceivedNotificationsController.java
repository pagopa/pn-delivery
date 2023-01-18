package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.RecipientReadApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class PnReceivedNotificationsController implements RecipientReadApi {
    private final NotificationRetrieverService retrieveSvc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final NotificationQRService notificationQRService;

    private final ModelMapperFactory modelMapperFactory;


    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc, NotificationAttachmentService notificationAttachmentService, NotificationQRService notificationQRService, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.notificationQRService = notificationQRService;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<NotificationSearchResponse> searchReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, OffsetDateTime startDate, OffsetDateTime endDate, List<String> xPagopaPnCxGroups, String mandateId, String senderId, NotificationStatus status, String subjectRegExp, String iunMatch, Integer size, String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_RCP, "searchReceivedNotification")
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
            ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class);
            response = mapper.map(serviceResult, NotificationSearchResponse.class);
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
                                                                                          NotificationStatus status,
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
            ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class);
            response = mapper.map(result, NotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException e) {
            log.error("can not search received delegated notification", e);
            logEvent.generateFailure("" + e.getProblem()).log();
            throw  e;
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<FullReceivedNotification> getReceivedNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        FullReceivedNotification result = null;
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_VIEW_RCP, "getReceivedNotification")
                .iun(iun)
                .build();
        logEvent.log();
        try {
            InternalNotification internalNotification = retrieveSvc.getNotificationAndNotifyViewedEvent(iun, xPagopaPnCxId, mandateId, xPagopaPnCxType.getValue(), xPagopaPnCxGroups);

            ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, FullReceivedNotification.class);

            result = mapper.map(internalNotification, FullReceivedNotification.class);

            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationDocument(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, Integer docIdx, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_DOCOPEN_RCP, "getReceivedNotificationDocument from documents array with index={}", docIdx)
                .iun(iun)
                .build();
        logEvent.log();



        try {
            response = notificationAttachmentService.downloadDocumentWithRedirect(
                    iun,
                    xPagopaPnCxType.toString(),
                    xPagopaPnCxId,
                    mandateId,
                    docIdx,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationDocument {}", message).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<NotificationAttachmentDownloadMetadataResponse> getReceivedNotificationAttachment(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String iun, String attachmentName, List<String> xPagopaPnCxGroups, String mandateId) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        NotificationAttachmentDownloadMetadataResponse response = new NotificationAttachmentDownloadMetadataResponse();
        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_ATCHOPEN_RCP, "getReceivedNotificationAttachment attachment name={}", attachmentName)
                .iun(iun)
                .build();
        logEvent.log();

        if(xPagopaPnCxType == CxTypeAuthFleet.PG && !(CollectionUtils.isEmpty(xPagopaPnCxGroups)))
            log.info("Calling pn-mandate to check if this specific notification has some mandate");

        try {
            response = notificationAttachmentService.downloadAttachmentWithRedirect(
                    iun,
                    xPagopaPnCxType.toString(),
                    xPagopaPnCxId,
                    mandateId,
                    null,
                    attachmentName,
                    true
            );
            String fileName = response.getFilename();
            String url = response.getUrl();
            String retryAfter = String.valueOf( response.getRetryAfter() );
            String message = LogUtils.createAuditLogMessageForDownloadDocument(fileName, url, retryAfter);
            logEvent.generateSuccess("getReceivedNotificationAttachment attachment name={}, {}",
                    attachmentName, message).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
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
                .mdcEntry( "aarQrCodeValue", aarQrCodeValue )
                .build();
        logEvent.log();
        ResponseCheckAarMandateDto responseCheckAarMandateDto;
        try {
            responseCheckAarMandateDto = notificationQRService.getNotificationByQRWithMandate( requestCheckAarMandateDto, xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnCxGroups );
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification by qr= " + exc.getProblem()).log();
            throw exc;
        }

        return ResponseEntity.ok( responseCheckAarMandateDto );
    }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleIdConflict;
import it.pagopa.pn.delivery.rest.utils.HandleRuntimeException;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;


@Slf4j
@RestController
public class PnNotificationInputController implements NewNotificationApi {

    public static final String NOTIFICATION_VALIDATION_ERROR_STATUS = "Notification validation error";
    private final PnDeliveryConfigs cfgs;
    private final NotificationReceiverService svc;
    private final NotificationAttachmentService notificationAttachmentService;

    public PnNotificationInputController(PnDeliveryConfigs cfgs, NotificationReceiverService svc, NotificationAttachmentService notificationAttachmentService) {
        this.cfgs = cfgs;
        this.svc = svc;
        this.notificationAttachmentService = notificationAttachmentService;
    }

    @Override
    public ResponseEntity<NewNotificationResponse> sendNewNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, NewNotificationRequest newNotificationRequest, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();

        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_INSERT, "sendNewNotification")
                .uid(xPagopaPnUid)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .build();
        NewNotificationResponse svcRes = null;
        try {
            svcRes = svc.receiveNotification(xPagopaPnCxId, newNotificationRequest);
        } catch (Exception ex) {
            logEvent.generateFailure(ex.getMessage()).log();
            throw ex;
        }
        logEvent.generateSuccess().log();
        return ResponseEntity.accepted().body( svcRes );
    }


    @Override
    public ResponseEntity<List<PreLoadResponse>> presignedUploadRequest(
            String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<PreLoadRequest> preLoadRequest) {
        Integer numberOfPresignedRequest = cfgs.getNumberOfPresignedRequest();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_PRELOAD, "presignedUploadRequest")
                .uid(xPagopaPnUid)
                .cxId(xPagopaPnCxId)
                .cxType(xPagopaPnCxType.toString())
                .build();
        if (preLoadRequest.size() > numberOfPresignedRequest) {
            String logMessage = String.format("Presigned upload request lenght=%d is more than maximum allowed=%d",
                    preLoadRequest.size(), numberOfPresignedRequest);
            log.error(logMessage);
            logEvent.generateFailure(logMessage).log();
            throw new PnValidationException("request", Collections.singleton(new ConstraintViolationImpl<>(String.format(logMessage))));
        }
        logEvent.generateSuccess().log();
        return ResponseEntity.ok(this.notificationAttachmentService.preloadDocuments(preLoadRequest));
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex) {
        return HandleValidation.handleValidationException(ex, NOTIFICATION_VALIDATION_ERROR_STATUS);
    }

    @ExceptionHandler({IdConflictException.class})
    public ResponseEntity<Problem> handleIdConflictException(IdConflictException ex) {
        return HandleIdConflict.handleIdConflictException( ex );
    }

    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Problem> handleRuntimeException( RuntimeException ex ) {
        return HandleRuntimeException.handleRuntimeException( ex );
    }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, String> logDetailsMap = new HashMap<>() {
            {
                put("xPagoPaPnUid", xPagopaPnUid);
                put("xPagopaPnCxType", xPagopaPnCxType.toString());
                put("xPagopaPnCxId", xPagopaPnCxId);
            }
        };
        NewNotificationResponse svcRes = svc.receiveNotification(xPagopaPnCxId, newNotificationRequest, logDetailsMap);
        return ResponseEntity.ok(svcRes);
    }


    @Override
    public ResponseEntity<List<PreLoadResponse>> presignedUploadRequest(
            String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<PreLoadRequest> preLoadRequest) {
        Integer numberOfPresignedRequest = cfgs.getNumberOfPresignedRequest();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        Map<String, String> logDetailsMap = new HashMap<>() {
            {
                put("xPagoPaPnUid", xPagopaPnUid);
                put("xPagopaPnCxType", xPagopaPnCxType.toString());
                put("xPagopaPnCxId", xPagopaPnCxId);
            }
        };
        if (preLoadRequest.size() > numberOfPresignedRequest) {
            String logMessage = String.format("Presigned upload request lenght=%s is more than maximum allowed=%s",
                    preLoadRequest.size(), numberOfPresignedRequest);
            log.error(logMessage);
            PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_PRELOAD, logMessage, logDetailsMap);
            logEvent.generateFailure(logMessage, logDetailsMap).log();
            throw new PnValidationException("request", Collections.singleton(new ConstraintViolationImpl<>(String.format(logMessage))));
        }
        return ResponseEntity.ok(this.notificationAttachmentService.preloadDocuments(preLoadRequest, logDetailsMap));
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex) {
        return HandleValidation.handleValidationException(ex, NOTIFICATION_VALIDATION_ERROR_STATUS);
    }
}

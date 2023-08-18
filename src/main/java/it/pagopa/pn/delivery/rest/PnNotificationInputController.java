package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_SIZE;


@Slf4j
@RestController
public class PnNotificationInputController implements NewNotificationApi {

    private final PnDeliveryConfigs cfgs;
    private final NotificationReceiverService svc;
    private final NotificationAttachmentService notificationAttachmentService;

    public PnNotificationInputController(PnDeliveryConfigs cfgs, NotificationReceiverService svc, NotificationAttachmentService notificationAttachmentService) {
        this.cfgs = cfgs;
        this.svc = svc;
        this.notificationAttachmentService = notificationAttachmentService;
    }

    @Override
    public ResponseEntity<NewNotificationResponse> sendNewNotification(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, NewNotificationRequest newNotificationRequest, List<String> xPagopaPnCxGroups){
        log.info("Nuova notifica ricevuta {}", newNotificationRequest);
        
        return ResponseEntity.ok(NewNotificationResponse.builder()
                .notificationRequestId("test").build());
    }

    @Override
    public ResponseEntity<List<PreLoadResponse>> presignedUploadRequest(
            String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<PreLoadRequest> preLoadRequest) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_PRELOAD, "presignedUploadRequest")
                .build();

        try {
            Integer numberOfPresignedRequest = cfgs.getNumberOfPresignedRequest();

            logEvent.log();
            if (preLoadRequest.size() > numberOfPresignedRequest) {
                String logMessage = String.format("Presigned upload request lenght=%d is more than maximum allowed=%d",
                        preLoadRequest.size(), numberOfPresignedRequest);
                log.error(logMessage);

                throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_SIZE,
                        "preLoadRequest", logMessage);
            }

            List<PreLoadResponse> res = this.notificationAttachmentService.preloadDocuments(preLoadRequest);
            String[] keys = res.stream().map(PreLoadResponse::getKey).toArray(String[]::new);
            String successMessage = "PreloadResponse file keys=" + String.join(", ", keys);
            logEvent.generateSuccess(successMessage).log();

            return ResponseEntity.ok(res);
        } catch (PnRuntimeException e) {
            logEvent.generateFailure("" + e.getProblem()).log();
            throw e;
        }
    }
}

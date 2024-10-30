package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NewNotificationApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.middleware.notificationdao.TaxonomyCodeDaoDynamo;
import it.pagopa.pn.delivery.models.TaxonomyCodeDto;
import it.pagopa.pn.delivery.svc.NotificationAttachmentService;
import it.pagopa.pn.delivery.svc.NotificationReceiverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.*;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;


@Slf4j
@RestController
public class PnNotificationInputController implements NewNotificationApi {

    private static final String DEFAULT_PAID = "default";
    private final PnDeliveryConfigs cfgs;
    private final NotificationReceiverService svc;
    private final NotificationAttachmentService notificationAttachmentService;
    private final TaxonomyCodeDaoDynamo taxonomyCodeDaoDynamo;

    public PnNotificationInputController(PnDeliveryConfigs cfgs, NotificationReceiverService svc, NotificationAttachmentService notificationAttachmentService, TaxonomyCodeDaoDynamo taxonomyCodeDaoDynamo) {
        this.cfgs = cfgs;
        this.svc = svc;
        this.notificationAttachmentService = notificationAttachmentService;
        this.taxonomyCodeDaoDynamo = taxonomyCodeDaoDynamo;
    }

    @Override
    public ResponseEntity<NewNotificationResponse> sendNewNotificationV23(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String xPagopaPnSrcCh, NewNotificationRequestV23 newNotificationRequest, List<String> xPagopaPnCxGroups, String xPagopaPnSrcChDetails, String xPagopaPnNotificationVersion) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        @NotNull String paProtocolNumber = newNotificationRequest.getPaProtocolNumber();
        String paIdempotenceToken = newNotificationRequest.getIdempotenceToken();
        String taxonomyCode = newNotificationRequest.getTaxonomyCode();

        PnAuditLogEvent logEvent = auditLogBuilder.before(PnAuditLogEventType.AUD_NT_INSERT, "sendNewNotification for protocolNumber={}, idempotenceToken={}", paProtocolNumber, paIdempotenceToken)
                .build();
        logEvent.log();
        NewNotificationResponse svcRes;

        if (taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(taxonomyCode, DEFAULT_PAID).isEmpty()) {
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_TAXONOMYCODE, taxonomyCode);
        }

        try {
            svcRes = svc.receiveNotification(xPagopaPnCxId, newNotificationRequest, xPagopaPnSrcCh, xPagopaPnSrcChDetails, xPagopaPnCxGroups, xPagopaPnNotificationVersion);
        } catch (PnRuntimeException ex) {
            logEvent.generateFailure("[protocolNumber={}, idempotenceToken={}] " + ex.getProblem(),
                    newNotificationRequest.getPaProtocolNumber(), paIdempotenceToken).log();
            throw ex;
        }
        @NotNull String requestId = svcRes.getNotificationRequestId();
        @NotNull String protocolNumber = svcRes.getPaProtocolNumber();
        String iun = new String(Base64Utils.decodeFromString(requestId), StandardCharsets.UTF_8);
        logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
        logEvent.generateSuccess("sendNewNotification requestId={}, protocolNumber={}, idempotenceToken={}", requestId, protocolNumber, paIdempotenceToken).log();
        return ResponseEntity.accepted().body( svcRes );
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

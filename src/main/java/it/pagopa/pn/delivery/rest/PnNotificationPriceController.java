package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NotificationPriceApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_TOPIC;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@Slf4j
@RestController
public class PnNotificationPriceController implements NotificationPriceApi {

    private final NotificationPriceService service;

    public PnNotificationPriceController(NotificationPriceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<NotificationPriceResponse> getNotificationPrice(String paTaxId, String noticeCode) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_REQCOST, "getNotificationPrice paTaxId={} noticeCode={}", paTaxId, noticeCode )
                .mdcEntry(MDC_PN_CTX_TOPIC, "paTaxId=" + paTaxId + ";noticeCode=" + noticeCode)
                .build();
        logEvent.log();
        NotificationPriceResponse response;
        try {
            response = service.getNotificationPrice( paTaxId, noticeCode );
            String iun = response.getIun();
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            logEvent.generateSuccess("getNotificationPrice paTaxId={}, noticeCode={}, iun={}, amount={}", paTaxId, noticeCode, iun, response.getAmount()).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification price= " + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }
}

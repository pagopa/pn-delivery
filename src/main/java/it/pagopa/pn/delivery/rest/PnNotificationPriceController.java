package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NotificationPriceV23Api;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponseV23;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_CTX_TOPIC;
import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@Slf4j
@RestController
public class PnNotificationPriceController implements NotificationPriceV23Api {

    private final NotificationPriceService service;

    public PnNotificationPriceController(NotificationPriceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<NotificationPriceResponseV23> getNotificationPriceV23(String paTaxId, String noticeCode) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_REQCOST, "getNotificationPrice paTaxId={} noticeCode={}", paTaxId, noticeCode )
                .mdcEntry(MDC_PN_CTX_TOPIC, String.format("paTaxId=%s;noticeCode=%s", paTaxId, noticeCode))
                .build();
        logEvent.log();
        NotificationPriceResponseV23 response;
        try {
            response = service.getNotificationPrice( paTaxId, noticeCode );
            String iun = response.getIun();
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            logEvent.generateSuccess("getNotificationPrice paTaxId={}, noticeCode={}, iun={}, partialPrice={}, totalPrice={}, paFee={}, sendFee={}, analogCost={}, vat={}",
                    paTaxId, noticeCode, iun, response.getPartialPrice(), response.getTotalPrice(), response.getPaFee(), response.getSendFee(), response.getAnalogCost(), response.getVat()).log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("Exception on get notification price= " + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }
}

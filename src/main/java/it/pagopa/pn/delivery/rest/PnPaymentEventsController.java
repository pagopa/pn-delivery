package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.PaymentEventsApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestPagoPa;
import it.pagopa.pn.delivery.svc.PaymentEventsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static it.pagopa.pn.commons.utils.MDCUtils.MDC_PN_IUN_KEY;

@RestController
@Slf4j
public class PnPaymentEventsController implements PaymentEventsApi {

    private final PaymentEventsService paymentEventsService;

    public PnPaymentEventsController(PaymentEventsService paymentEventsService) {
        this.paymentEventsService = paymentEventsService;
    }

    @Override
    public ResponseEntity<Void> paymentEventsRequestPagoPa(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, PaymentEventsRequestPagoPa paymentEventsRequestPagoPa, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_PAYMENT, "payment events request {}", paymentEventsRequestPagoPa)
                .build();
        logEvent.log();
        try {
            String iun = paymentEventsService.handlePaymentEventsPagoPa(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnCxGroups, paymentEventsRequestPagoPa);
            logEvent.getMdc().put(MDC_PN_IUN_KEY, iun);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

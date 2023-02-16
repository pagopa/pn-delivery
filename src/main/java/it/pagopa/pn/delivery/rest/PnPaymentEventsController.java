package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.PaymentEventsApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestPagoPa;
import it.pagopa.pn.delivery.svc.PaymentEventsService;
import it.pagopa.pn.delivery.utils.PaymentEventsLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PnPaymentEventsController implements PaymentEventsApi {

    private final PaymentEventsService paymentEventsService;
    private final PaymentEventsLogUtil paymentEventsLogUtil;

    public PnPaymentEventsController(PaymentEventsService paymentEventsService, PaymentEventsLogUtil paymentEventsLogUtil) {
        this.paymentEventsService = paymentEventsService;
        this.paymentEventsLogUtil = paymentEventsLogUtil;
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
            logEvent.getMdc().put("iun", iun);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Void> paymentEventsRequestF24(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, PaymentEventsRequestF24 paymentEventsRequestF24, List<String> xPagopaPnCxGroups) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_PAYMENT, "payment events request {}", paymentEventsLogUtil.maskRecipientTaxIdForLog( paymentEventsRequestF24) )
                .build();
        logEvent.log();
        try {
            paymentEventsService.handlePaymentEventsF24(xPagopaPnCxType.getValue(), xPagopaPnCxId, xPagopaPnCxGroups, paymentEventsRequestF24);
            logEvent.getMdc().put("iun", paymentEventsRequestF24.getEvents().get(0).getIun());
            logEvent.generateSuccess().log();

        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

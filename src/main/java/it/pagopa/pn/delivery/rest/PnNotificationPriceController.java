package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.NotificationPriceApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleNotFound;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.NotificationPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PnNotificationPriceController implements NotificationPriceApi {

    public static final String VALIDATION_ERROR_STATUS = "Notification price validation error";
    private final NotificationPriceService service;

    public PnNotificationPriceController(NotificationPriceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<NotificationPriceResponse> getNotificationPrice(String paTaxId, String noticeCode) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_REQCOST, "getNotificationPrice paTaxId={} noticeCode={}", paTaxId, noticeCode )
                .mdcEntry("paTaxId",paTaxId)
                .mdcEntry("noticeCode", noticeCode)
                .build();
        logEvent.log();
        NotificationPriceResponse response;
        try {
            response = service.getNotificationPrice( paTaxId, noticeCode );
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure("Exception on get notification price= " + exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );
    }
    @ExceptionHandler({javax.validation.ConstraintViolationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(javax.validation.ConstraintViolationException ex) {
        return HandleValidation.handleValidationException(ex, ex.getMessage());
    }

    @ExceptionHandler({PnNotFoundException.class})
    public ResponseEntity<ResErrorDto> handleNotFoundException(PnNotFoundException ex) {
        return HandleNotFound.handleNotFoundException( ex, ex.getMessage() );
    }
}

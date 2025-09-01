package it.pagopa.pn.delivery.springbootcfg;


import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnResponseEntityExceptionHandler;
import it.pagopa.pn.delivery.exception.PnIoMandateNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

@org.springframework.web.bind.annotation.ControllerAdvice
@Import(ExceptionHelper.class)
public class PnResponseEntityExceptionHandlerActivation extends PnResponseEntityExceptionHandler {
    public PnResponseEntityExceptionHandlerActivation(ExceptionHelper exceptionHelper) {
        super(exceptionHelper);
    }

    @ExceptionHandler({PnIoMandateNotFoundException.class})
    public ResponseEntity<ResponseCheckQrMandateDto> handleIoMandateNotFoundException(PnIoMandateNotFoundException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getResponseCheckQrMandateDto());
    }
}

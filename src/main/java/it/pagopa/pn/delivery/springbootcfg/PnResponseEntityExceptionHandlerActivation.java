package it.pagopa.pn.delivery.springbootcfg;


import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnResponseEntityExceptionHandler;
import it.pagopa.pn.delivery.exception.PnIoMandateNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.CheckQrForbiddenResponse;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.CheckQrForbiddenResponseDetail;
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
    public ResponseEntity<CheckQrForbiddenResponse> handleIoMandateNotFoundException(PnIoMandateNotFoundException ex) {
        CheckQrForbiddenResponse checkQrForbiddenResponse = buildCheckQrForbiddenResponse(ex.getResponseCheckQrMandateDto());
        return ResponseEntity.status(ex.getStatus()).body(checkQrForbiddenResponse);
    }

    private CheckQrForbiddenResponse buildCheckQrForbiddenResponse(ResponseCheckQrMandateDto responseCheckQrMandateDto) {
        return CheckQrForbiddenResponse.builder()
                .errorType(CheckQrForbiddenResponse.ErrorTypeEnum.MANDATE_NOT_PRESENT)
                .detail(CheckQrForbiddenResponseDetail.builder()
                        .iun(responseCheckQrMandateDto.getIun())
                        .recipientInfo(responseCheckQrMandateDto.getRecipientInfo())
                        .build())
                .build();
    }


}

package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnIoMandateNotFoundException extends PnRuntimeException {
    private final transient ResponseCheckQrMandateDto responseCheckQrMandateDto;

    public PnIoMandateNotFoundException(ResponseCheckQrMandateDto responseCheckQrMandateDto) {
        super("Can't access notification", "User is not recipient or delegated of notification", HttpStatus.FORBIDDEN.value(), "MISSING_MANDATE", "", "");
        this.responseCheckQrMandateDto = responseCheckQrMandateDto;
    }
}

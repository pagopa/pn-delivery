package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.delivery.exception.PnIoMandateNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class PnResponseEntityExceptionHandlerActivationTest {
    @Mock
    private ExceptionHelper exceptionHelper;
    private PnResponseEntityExceptionHandlerActivation handler;

    @BeforeEach
    void setup() {
        exceptionHelper = Mockito.mock(ExceptionHelper.class);
        handler = new PnResponseEntityExceptionHandlerActivation( exceptionHelper);
    }

    @Test
    void handleIoMandateNotFoundException() {
        PnIoMandateNotFoundException exception = new PnIoMandateNotFoundException(new ResponseCheckQrMandateDto());
        ResponseEntity<ResponseCheckQrMandateDto> response = handler.handleIoMandateNotFoundException(exception);
        assertNotNull(response);
        assertEquals(403, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

}
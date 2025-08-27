package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PnIoMandateNotFoundExceptionTest {
    @Test
    void testConstructor() {
        ResponseCheckQrMandateDto responseCheckQrMandateDto = new ResponseCheckQrMandateDto();
        PnIoMandateNotFoundException exception = new PnIoMandateNotFoundException(responseCheckQrMandateDto);

        assertEquals("Can't access notification", exception.getMessage());
        assertEquals(403, exception.getStatus());
        assertEquals(responseCheckQrMandateDto, exception.getResponseCheckQrMandateDto());
    }
}
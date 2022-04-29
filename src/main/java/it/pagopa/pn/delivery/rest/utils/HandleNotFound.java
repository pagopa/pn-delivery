package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import org.springframework.http.ResponseEntity;

public class HandleNotFound {
    private HandleNotFound(){}

    public static ResponseEntity<ResErrorDto> handleNotFoundException(PnNotFoundException ex, String statusError) {
        return ResponseEntity.notFound().build();
    }
}

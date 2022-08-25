package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import org.springframework.http.ResponseEntity;

public class HandleBadRequest {
    private HandleBadRequest(){}

    public static ResponseEntity<ResErrorDto> handleBadRequestException(PnBadRequestException ex, String statusError) {
        return ResponseEntity.badRequest().build();
    }
}

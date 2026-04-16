package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnCampaignException extends PnRuntimeException {

    public PnCampaignException(String message, String description, String errorcode) {
        super(message, description, HttpStatus.NOT_FOUND.value(), errorcode, null, null);
    }

}

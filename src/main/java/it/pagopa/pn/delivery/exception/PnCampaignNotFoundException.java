package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnCampaignNotFoundException extends PnRuntimeException {
    private static final String ERROR_CODE = "PN_DELIVERY_CAMPAIGN_NOT_FOUND";

    public PnCampaignNotFoundException(String message, String description) {
        super(message, description, HttpStatus.NOT_FOUND.value(), ERROR_CODE, null, null);
    }

}

package it.pagopa.pn.delivery.exception;

import lombok.Getter;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_CAMPAIGN_NOT_FOUND;

@Getter
public class PnCampaignNotFoundException extends PnNotFoundException {

    public PnCampaignNotFoundException(String description) {
        super("Campaign not found", description, ERROR_CODE_DELIVERY_CAMPAIGN_NOT_FOUND);
    }
}


package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;

public record NotificationCostRequest(String iun, int recipientIdx, NotificationFeePolicy notificationFeePolicy,
                                      Boolean applyCost, Integer paFee, Integer vat, String iuv) {
}

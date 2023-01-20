package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class PaymentEventsLogUtil {

    @NotNull
    public String maskRecipientTaxIdForLog( PaymentEventsRequestF24 paymentEventsRequestF24 ) {
        PaymentEventsRequestF24 copy = new PaymentEventsRequestF24( paymentEventsRequestF24.getEvents() );
        copy.getEvents().forEach(
                e -> e.setRecipientTaxId( LogUtils.maskTaxId( e.getRecipientTaxId() ) )
        );
        return copy.toString();

    }
}

package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.utils.LogUtils;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

@Component
public class PaymentEventsLogUtil {

    @NotNull
    public String maskRecipientTaxIdForLog( PaymentEventsRequestF24 paymentEventsRequestF24 ) {
        List<PaymentEventF24> paymentEventF24Copy = paymentEventsRequestF24.getEvents().stream().map(
                e -> PaymentEventF24.builder()
                        .recipientType(e.getRecipientType())
                        .iun(e.getIun())
                        .recipientTaxId( LogUtils.maskTaxId( e.getRecipientTaxId() ))
                        .paymentDate(e.getPaymentDate())
                        .build()
        ).toList();
        return PaymentEventsRequestF24.builder()
                .events( paymentEventF24Copy )
                .build()
                .toString();
    }
}

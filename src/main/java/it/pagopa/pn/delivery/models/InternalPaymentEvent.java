package it.pagopa.pn.delivery.models;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class InternalPaymentEvent {
    private String iun;
    private String recipientTaxId;
    private String recipientInternalId;
    private int recipientIdx;
    private PnDeliveryPaymentEvent.RecipientType recipientType;
    private Instant paymentDate;
    private PnDeliveryPaymentEvent.PaymentType paymentType;
    private String creditorTaxId;
    private String noticeCode;
}

package it.pagopa.pn.delivery.models;

import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import lombok.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class InternalPaymentEvent {
    @NotNull
    private String iun;
    @NotNull
    private int recipientIdx;
    @NotNull
    private PnDeliveryPaymentEvent.RecipientType recipientType;
    @NotNull
    private Instant paymentDate;
    @NotNull
    private PnDeliveryPaymentEvent.PaymentType paymentType;
    @Nullable
    private String creditorTaxId;
    @Nullable
    private String noticeCode;
}

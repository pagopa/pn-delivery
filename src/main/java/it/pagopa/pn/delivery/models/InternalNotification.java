package it.pagopa.pn.delivery.models;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotificationV20;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true, builderMethodName = "fullSentNotificationBuilder")
@ToString
public class InternalNotification extends FullSentNotificationV20 {
    public InternalNotification(FullSentNotificationV20 fsn) {
        super(
                fsn.getIdempotenceToken(),
                fsn.getPaProtocolNumber(),
                fsn.getSubject(),
                fsn.getAbstract(),
                fsn.getRecipients(),
                fsn.getDocuments(),
                fsn.getNotificationFeePolicy(),
                fsn.getCancelledIun(),
                fsn.getPhysicalCommunicationType(),
                fsn.getSenderDenomination(),
                fsn.getSenderTaxId(),
                fsn.getGroup(),
                fsn.getAmount(),
                fsn.getPaymentExpirationDate(),
                fsn.getTaxonomyCode(),
                fsn.getPagoPaIntMode(),
                fsn.getSenderPaId(),
                fsn.getIun(),
                fsn.getSentAt(),
                fsn.getCancelledByIun(),
                fsn.getDocumentsAvailable(),
                fsn.getNotificationStatus(),
                fsn.getNotificationStatusHistory(),
                fsn.getTimeline(),
                fsn.getRecipientIds(),
                fsn.getSourceChannel(),
                fsn.getSourceChannelDetails()
            );
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

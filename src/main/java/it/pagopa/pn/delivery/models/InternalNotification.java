package it.pagopa.pn.delivery.models;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true, builderMethodName = "fullSentNotificationBuilder")
@ToString
public class InternalNotification extends FullSentNotification {
    public InternalNotification(FullSentNotification fsn) {
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
                fsn.getSenderPaId(),
                fsn.getIun(),
                fsn.getSentAt(),
                fsn.getCancelledByIun(),
                fsn.getDocumentsAvailable(),
                fsn.getNotificationStatus(),
                fsn.getNotificationStatusHistory(),
                fsn.getTimeline(),
                fsn.getRecipientIds(),
                fsn.getSourceChannel()
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

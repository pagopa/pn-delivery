package it.pagopa.pn.delivery.models;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true, builderMethodName = "fullSentNotificationBuilder")
@ToString
public class InternalNotification extends FullSentNotificationPrivate {


    public InternalNotification(FullSentNotificationPrivate fsn, List<String> recipientIds, String sourceChannel) {
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
                fsn.getTimeline()
            );
        this.recipientIds = recipientIds;
        this.sourceChannel = sourceChannel;
    }

    //@Schema( description = "Lista degli id dei destinatari")
    private List<String> recipientIds;

    public List<String> getRecipientIds() { return this.recipientIds; }

    public void setRecipientIds( List<String> recipientIds ) { this.recipientIds = recipientIds; }

    private String sourceChannel;

    public String getSourceChannel() { return this.sourceChannel; }

    public void setSourceChannel( String sourceChannel ) { this.sourceChannel = sourceChannel; }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

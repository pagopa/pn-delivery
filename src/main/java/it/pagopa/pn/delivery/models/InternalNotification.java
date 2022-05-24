package it.pagopa.pn.delivery.models;


import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true, builderMethodName = "fullSentNotificationBuilder")
@ToString
@Schema(
        description = "Le notifiche di Piattaforma Notifiche",
        externalDocs = @ExternalDocumentation( description = "MarkDown", url = "http://google.it/"))
public class InternalNotification extends FullSentNotification {


    public InternalNotification(FullSentNotification fsn, Map<NotificationRecipient, String> tokens, List<String> recipientIds) {
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
                fsn.getSenderPaId(),
                fsn.getIun(),
                fsn.getSentAt(),
                fsn.getCancelledByIun(),
                fsn.getDocumentsAvailable(),
                fsn.getNotificationStatus(),
                fsn.getNotificationStatusHistory(),
                fsn.getTimeline()
            );
        this.tokens = tokens;
        this.recipientIds = recipientIds;
    }

    @Schema( description = "Lista dei token generati per ogni destinatario")
    private Map<NotificationRecipient,String> tokens;

    public String getToken( NotificationRecipient recipient ){
        return tokens.get( recipient );
    }

    @Schema( description = "Lista degli id dei destinatari")
    private List<String> recipientIds;

    public List<String> getRecipientIds() { return this.recipientIds; }

    public void setRecipientIds( List<String> recipientIds ) { this.recipientIds = recipientIds; }

}

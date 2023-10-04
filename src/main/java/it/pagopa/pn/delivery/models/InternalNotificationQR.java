package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class InternalNotificationQR {
    private NotificationRecipientV21.RecipientTypeEnum recipientType;
    private String recipientInternalId;
    private String aarQRCodeValue;
    private String iun;
}

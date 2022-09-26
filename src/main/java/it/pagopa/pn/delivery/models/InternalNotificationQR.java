package it.pagopa.pn.delivery.models;

import it.pagopa.pn.api.dto.notification.NotificationRecipientType;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class InternalNotificationQR {
    private NotificationRecipientType recipientType;
    private String recipientInternalId;
    private String aarQRCodeValue;
    private String iun;
}

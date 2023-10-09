package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DelegateInfo {
    private String internalId;
    private String taxId;
    private String operatorUuid;
    private String mandateId;
    private String denomination;
    private NotificationRecipientV21.RecipientTypeEnum delegateType;
}

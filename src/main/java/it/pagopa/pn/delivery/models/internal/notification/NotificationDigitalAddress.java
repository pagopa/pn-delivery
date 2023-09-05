package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress.TypeEnum;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDigitalAddress {
    private TypeEnum type;
    private String address;
}

package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SendingReceipt {
    private String id;
    private String system;
}

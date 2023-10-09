package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAttachmentBodyRef {
    private String key;
    private String versionToken;

}

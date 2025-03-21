package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDocument   {
    private NotificationAttachmentDigests digests;
    private String contentType;
    private NotificationAttachmentBodyRef ref;
    private String title;
    private String docIdx;
}

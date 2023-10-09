package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

import java.time.OffsetDateTime;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDetails {
    private String id;
    private String documentType;
    private String url;
    private OffsetDateTime date;
}

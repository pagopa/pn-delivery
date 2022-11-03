package it.pagopa.pn.delivery.svc.preloaded_digest_error;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationAttachmentDigests;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@ToString
@DigestEquality
public class DigestEqualityBean {

    private String key;
    private NotificationAttachmentDigests expected;
    private NotificationAttachmentDigests actual;
}

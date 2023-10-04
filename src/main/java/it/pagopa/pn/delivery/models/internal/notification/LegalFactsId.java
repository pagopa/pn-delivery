package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LegalFactCategory;
import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LegalFactsId {
    private String key;
    private LegalFactCategory category;
}

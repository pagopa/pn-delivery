package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategoryV20;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TimelineElement {
    private String elementId;
    private OffsetDateTime timestamp;
    private List<LegalFactsId> legalFactsIds = null;
    private TimelineElementCategoryV20 category;
    private TimelineElementDetails details;
}

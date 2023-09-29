package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusHistoryElement {
    private NotificationStatus status;
    private OffsetDateTime activeFrom;
    private List<String> relatedTimelineElements = new ArrayList<>();

}

package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusHistoryElementV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1;
import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class InformalNotificationDetail implements NotificationDetail {
    private InternalNotification notification;
    private InformalNotificationStatusV1 notificationStatus;
    private List<InformalNotificationStatusHistoryElementV1> notificationStatusHistory;
    private List<InformalTimelineElementV1> timeline;
}

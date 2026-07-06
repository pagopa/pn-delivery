package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV28;
import lombok.*;

import java.util.List;


@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class LegalNotificationDetail implements NotificationDetail {
    private InternalNotification notification;
    private NotificationStatusV26 notificationStatus;
    private List<NotificationStatusHistoryElementV26> notificationStatusHistory;
    private List<TimelineElementV28> timeline;
}

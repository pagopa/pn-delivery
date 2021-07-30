package it.pagopa.pn.delivery.model.notification;


import java.util.List;
import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.rest.JsonViews;

@Table
@Data
@Builder(toBuilder = true)
public class Notification {

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    @PrimaryKey
    private String iun;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private String paNotificationId;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private String subject;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private String cancelledIun;

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    private String cancelledByIun;

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    private NotificationSender sender ;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private List<NotificationRecipient> recipients ;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private List<NotificationAttachment> documents ;

    @JsonView(value = { JsonViews.NotificationsView.ReceivedNotification.class, JsonViews.NotificationsView.Sent.class })
    private NotificationPaymentInfo payment;

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    private NotificationStatus notificationStatus;

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    private List<NotificationStatusHistoryElement> notificationStatusHistory;

    @JsonView(value = { JsonViews.None.class, JsonViews.NotificationsView.Sent.class })
    private List<TimelineElement> timeline;

}

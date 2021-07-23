package it.pagopa.pn.delivery.model.notification;


import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.rest.Views;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.List;


@Table
@Data
@Builder
public class Notification {

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    @PrimaryKey
    private String iun;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private String paNotificationId;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private String subject;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private String cancelledIun;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private String cancelledByIun;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private NotificationSender sender ;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private List<NotificationRecipient> recipients ;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private List<NotificationAttachment> documents ;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private NotificationPaymentInfo payment;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private NotificationStatus notificationStatus;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private List<NotificationStatusHistoryElement> notificationStatusHistory;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private List<TimelineElement> timeline ;

}

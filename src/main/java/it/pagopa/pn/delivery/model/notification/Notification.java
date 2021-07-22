package it.pagopa.pn.delivery.model.notification;


import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.rest.Views;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.ArrayList;
import java.util.List;

@Data
@Table
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
    private NotificationSender sender = new NotificationSender();

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private List<NotificationRecipient> recipients;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private List<NotificationAttachment> documents;

    @JsonView(value = { Views.NotificationsView.Send.class, Views.NotificationsView.Sent.class })
    private NotificationPaymentInfo payment;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private NotificationStatus notificationStatus;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private List<NotificationStatusHistoryElement> notificationStatusHistory;

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
    private List<TimelineElement> timeline = new ArrayList<>();

}

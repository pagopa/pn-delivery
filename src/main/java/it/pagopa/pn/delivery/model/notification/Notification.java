package it.pagopa.pn.delivery.model.notification;


import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.rest.Views;

import java.util.ArrayList;
import java.util.List;

public class Notification {

    @JsonView(value = { Views.None.class, Views.NotificationsView.Sent.class })
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

    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    public String getPaNotificationId() {
        return paNotificationId;
    }

    public void setPaNotificationId(String paNotificationId) {
        this.paNotificationId = paNotificationId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCancelledIun() {
        return cancelledIun;
    }

    public void setCancelledIun(String cancelledIun) {
        this.cancelledIun = cancelledIun;
    }

    public String getCancelledByIun() {
        return cancelledByIun;
    }

    public void setCancelledByIun(String cancelledByIun) {
        this.cancelledByIun = cancelledByIun;
    }

    public NotificationSender getSender() {
        return sender;
    }

    public void setSender(NotificationSender sender) {
        this.sender = sender;
    }

    public List<NotificationRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipient> recipients) {
        this.recipients = recipients;
    }

    public List<NotificationAttachment> getDocuments() {
        return documents;
    }

    public void setDocuments(List<NotificationAttachment> documents) {
        this.documents = documents;
    }

    public NotificationPaymentInfo getPayment() {
        return payment;
    }

    public void setPayment(NotificationPaymentInfo payment) {
        this.payment = payment;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public List<NotificationStatusHistoryElement> getNotificationStatusHistory() {
        return notificationStatusHistory;
    }

    public void setNotificationStatusHistory(List<NotificationStatusHistoryElement> notificationStatusHistory) {
        this.notificationStatusHistory = notificationStatusHistory;
    }

    public List<TimelineElement> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineElement> timeline) {
        this.timeline = timeline;
    }
}

package it.pagopa.pn.delivery.middleware.model.notification;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.Map;

public class NotificationMetadataEntity {

    public static final String NOTIFICATIONS_METADATA_TABLE_NAME = "NotificationsMetadata";

    private String iun_recipientId;
    private Instant sentAt;
    private String senderId;
    private String recipientId;
    private boolean recipientOne;
    private String notificationGroup;
    private String notificationStatus;
    private Map<String,String> tableRow;
    private String senderId_creationMonth;
    private String recipientId_creationMonth;
    private String senderId_recipientId;

    @DynamoDbPartitionKey
    public String getIun_recipientId() {
        return iun_recipientId;
    }

    public void setIun_recipientId(String iun_recipientId) {
        this.iun_recipientId = iun_recipientId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute(value = "sentAt")
    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @DynamoDbAttribute(value = "senderId")
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @DynamoDbAttribute(value = "recipientId")
    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    @DynamoDbAttribute(value = "recipientOne")
    public boolean isRecipientOne() {
        return recipientOne;
    }

    public void setRecipientOne(boolean recipientOne) {
        this.recipientOne = recipientOne;
    }

    @DynamoDbAttribute(value = "notificationGroup")
    public String getNotificationGroup() {
        return notificationGroup;
    }

    public void setNotificationGroup(String notificationGroup) {
        this.notificationGroup = notificationGroup;
    }

    @DynamoDbAttribute(value = "notificationStatus")
    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    @DynamoDbAttribute(value = "tableRow")
    public Map<String, String> getTableRow() {
        return tableRow;
    }

    public void setTableRow(Map<String, String> tableRow) {
        this.tableRow = tableRow;
    }

    @DynamoDbAttribute(value = "senderId_creationMonth")
    public String getSenderId_creationMonth() {
        return senderId_creationMonth;
    }

    public void setSenderId_creationMonth(String senderId_creationMonth) {
        this.senderId_creationMonth = senderId_creationMonth;
    }

    @DynamoDbAttribute(value = "recipientId_creationMonth")
    public String getRecipientId_creationMonth() {
        return recipientId_creationMonth;
    }

    public void setRecipientId_creationMonth(String recipientId_creationMonth) {
        this.recipientId_creationMonth = recipientId_creationMonth;
    }

    @DynamoDbAttribute(value = "senderId_recipientId")
    public String getSenderId_recipientId() {
        return senderId_recipientId;
    }

    public void setSenderId_recipientId(String senderId_recipientId) {
        this.senderId_recipientId = senderId_recipientId;
    }
}

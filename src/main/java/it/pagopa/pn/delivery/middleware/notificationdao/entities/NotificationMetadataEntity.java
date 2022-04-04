package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationMetadataEntity {

    public static final String FIELD_IUN_RECIPIENT_ID = "iun_recipientId";
    public static final String FIELD_SENT_AT = "sentAt";
    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_RECIPIENT_ID = "recipientId";
    public static final String INDEX_SENDER_ID = "senderId";
    public static final String INDEX_SENDER_ID_RECIPIENT_ID = "senderId_recipientId";
    public static final String INDEX_RECIPIENT_ID = "recipientId";
    public static final String FIELD_RECIPIENT_IDS = "recipientIds";
    public static final String FIELD_RECIPIENT_ONE = "recipientOne";
    public static final String FIELD_NOTIFICATION_GROUP = "notificationGroup";
    public static final String FIELD_NOTIFICATION_STATUS = "notificationStatus";
    public static final String FIELD_TABLE_ROW = "tableRow";
    public static final String FIELD_SENDER_ID_CREATION_MONTH = "senderId_creationMonth";
    public static final String FIELD_RECIPIENT_ID_CREATION_MONTH = "recipientId_creationMonth";
    public static final String FIELD_SENDER_ID_RECIPIENT_ID = "senderId_recipientId";


    private String iun_recipientId;
    private Instant sentAt;
    private String senderId;
    private String recipientId;
    private List<String> recipientIds;
    private boolean recipientOne;
    private String notificationGroup;
    private String notificationStatus;
    private Map<String,String> tableRow;
    private String senderId_creationMonth;
    private String recipientId_creationMonth;
    private String senderId_recipientId;


    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_IUN_RECIPIENT_ID)
    public String getIun_recipientId() {
        return iun_recipientId;
    }

    public void setIun_recipientId(String iun_recipientId) {
        this.iun_recipientId = iun_recipientId;
    }

    @DynamoDbSortKey
    @DynamoDbSecondarySortKey( indexNames = {INDEX_SENDER_ID, INDEX_SENDER_ID_RECIPIENT_ID, INDEX_RECIPIENT_ID})
    @DynamoDbAttribute(value = FIELD_SENT_AT)
    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @DynamoDbAttribute(value = FIELD_SENDER_ID)
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENT_ID)
    public String getRecipientId() { return recipientId; }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENT_IDS)
    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENT_ONE)
    public boolean isRecipientOne() {
        return recipientOne;
    }

    public void setRecipientOne(boolean recipientOne) {
        this.recipientOne = recipientOne;
    }

    @DynamoDbAttribute(value = FIELD_NOTIFICATION_GROUP)
    public String getNotificationGroup() {
        return notificationGroup;
    }

    public void setNotificationGroup(String notificationGroup) {
        this.notificationGroup = notificationGroup;
    }

    @DynamoDbAttribute(value = FIELD_NOTIFICATION_STATUS)
    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    @DynamoDbAttribute(value = FIELD_TABLE_ROW)
    public Map<String, String> getTableRow() {
        return tableRow;
    }

    public void setTableRow(Map<String, String> tableRow) {
        this.tableRow = tableRow;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = { INDEX_SENDER_ID })
    @DynamoDbAttribute(value = FIELD_SENDER_ID_CREATION_MONTH)
    public String getSenderId_creationMonth() {
        return senderId_creationMonth;
    }

    public void setSenderId_creationMonth(String senderId_creationMonth) {
        this.senderId_creationMonth = senderId_creationMonth;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = { INDEX_RECIPIENT_ID })
    @DynamoDbAttribute(value = FIELD_RECIPIENT_ID_CREATION_MONTH)
    public String getRecipientId_creationMonth() {
        return recipientId_creationMonth;
    }

    public void setRecipientId_creationMonth(String recipientId_creationMonth) {
        this.recipientId_creationMonth = recipientId_creationMonth;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = { INDEX_SENDER_ID_RECIPIENT_ID })
    @DynamoDbAttribute(value = FIELD_SENDER_ID_RECIPIENT_ID)
    public String getSenderId_recipientId() {
        return senderId_recipientId;
    }

    public void setSenderId_recipientId(String senderId_recipientId) {
        this.senderId_recipientId = senderId_recipientId;
    }
}

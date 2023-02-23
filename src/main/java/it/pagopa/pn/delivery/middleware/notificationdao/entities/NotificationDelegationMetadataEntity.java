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
@Setter
@DynamoDbBean
public class NotificationDelegationMetadataEntity {

    public static final String FIELD_IUN_RECIPIENT_ID_DELEGATE_ID_GROUP_ID = "iun_recipientId_delegateId_groupId";
    public static final String FIELD_SENT_AT = "sentAt";
    public static final String FIELD_DELEGATE_ID_CREATION_MONTH = "delegateId_creationMonth";
    public static final String FIELD_DELEGATE_ID_GROUP_ID_CREATION_MONTH = "delegateId_groupId_creationMonth";
    public static final String FIELD_MANDATE_ID = "mandateId";
    public static final String FIELD_SENDER_ID = "senderId";
    public static final String FIELD_RECIPIENT_ID = "recipientId";
    public static final String FIELD_RECIPIENT_IDS = "recipientIds";
    public static final String FIELD_NOTIFICATION_STATUS = "notificationStatus";
    public static final String FIELD_SENDER_ID_CREATION_MONTH = "senderId_creationMonth";
    public static final String FIELD_RECIPIENT_ID_CREATION_MONTH = "recipientId_creationMonth";
    public static final String FIELD_SENDER_ID_RECIPIENT_ID = "senderId_recipientId";
    public static final String FIELD_TABLE_ROW = "tableRow";

    public static final String INDEX_DELEGATE_ID = "delegateId";
    public static final String INDEX_DELEGATE_ID_GROUP_ID = "delegateId_groupId";
    public static final String INDEX_MANDATE_ID = "mandateId";

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(value = FIELD_IUN_RECIPIENT_ID_DELEGATE_ID_GROUP_ID)
    }))
    private String iunRecipientIdDelegateIdGroupId;

    @Getter(onMethod = @__({
            @DynamoDbSortKey,
            @DynamoDbSecondarySortKey(indexNames = {INDEX_DELEGATE_ID, INDEX_DELEGATE_ID_GROUP_ID}),
            @DynamoDbAttribute(value = FIELD_SENT_AT)
    }))
    private Instant sentAt;

    @Getter(onMethod = @__({
            @DynamoDbSecondaryPartitionKey(indexNames = {INDEX_DELEGATE_ID}),
            @DynamoDbAttribute(value = FIELD_DELEGATE_ID_CREATION_MONTH)
    }))
    private String delegateIdCreationMonth;

    @Getter(onMethod = @__({
            @DynamoDbSecondaryPartitionKey(indexNames = {INDEX_DELEGATE_ID_GROUP_ID}),
            @DynamoDbAttribute(value = FIELD_DELEGATE_ID_GROUP_ID_CREATION_MONTH)
    }))
    private String delegateIdGroupIdCreationMonth;

    @Getter(onMethod = @__({
            @DynamoDbSecondaryPartitionKey(indexNames = {INDEX_MANDATE_ID}),
            @DynamoDbAttribute(value = FIELD_MANDATE_ID)
    }))
    private String mandateId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_SENDER_ID)
    }))
    private String senderId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_RECIPIENT_ID)
    }))
    private String recipientId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_RECIPIENT_IDS)
    }))
    private List<String> recipientIds;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_NOTIFICATION_STATUS)
    }))
    private String notificationStatus;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_SENDER_ID_CREATION_MONTH)
    }))
    private String senderIdCreationMonth;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_RECIPIENT_ID_CREATION_MONTH)
    }))
    private String recipientIdCreationMonth;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_SENDER_ID_RECIPIENT_ID)
    }))
    private String senderIdRecipientId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(value = FIELD_TABLE_ROW)
    }))
    private Map<String, String> tableRow;

}

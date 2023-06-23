package it.pagopa.pn.delivery.middleware.notificationdao.entities;


import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationFeePolicy;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class NotificationEntity {
    public static final String FIELD_IUN = "iun";
    public static final String FIELD_ABSTRACT = "abstract";
    public static final String FIELD_IDEMPOTENCE_TOKEN = "idempotenceToken";
    public static final String FIELD_PA_NOTIFICATION_ID = "paNotificationId";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_SENT_AT = "sentAt";
    public static final String FIELD_CANCELLED_IUN = "cancelledIun";
    public static final String FIELD_CANCELLED_BY_IUN = "cancelledByIun";
    public static final String FIELD_SENDER_PA_ID = "senderPaId";
    public static final String FIELD_RECIPIENTS = "recipients";
    public static final String FIELD_RECIPIENT_IDS = "recipientIds";
    public static final String FIELD_NOTIFICATION_FEE_POLICY = "notificationFeePolicy";
    public static final String FIELD_PHYSICAL_COMMUNICATION_TYPE = "physicalCommunicationType";
    public static final String FIELD_GROUP = "group";
    public static final String FIELD_SENDER_DENOMINATION = "senderDenomination";
    public static final String FIELD_SENDER_TAX_ID = "senderTaxId";
    public static final String FIELD_DOCUMENTS = "documents";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_PAYMENT_EXPIRATION_DATE = "paymentExpirationDate";
    public static final String FIELD_REQUEST_ID = "requestId";
    public static final String FIELD_TAXONOMY_CODE = "taxonomyCode";
    public static final String FIELD_SOURCE_CHANNEL = "sourceChannel";
    public static final String FIELD_SOURCE_CHANNEL_DETAILS = "sourceChannelDetails";
    public static final String FIELD_VERSION = "version";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_IUN)})) private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_ABSTRACT)})) private String notificationAbstract;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_IDEMPOTENCE_TOKEN)})) private String idempotenceToken;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PA_NOTIFICATION_ID)})) private String paNotificationId;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SUBJECT)})) private String subject;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SENT_AT)})) private Instant sentAt;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_CANCELLED_IUN)})) private String cancelledIun;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_CANCELLED_BY_IUN)})) private String cancelledByIun;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SENDER_PA_ID)})) private String senderPaId;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_RECIPIENTS)})) private List<NotificationRecipientEntity> recipients;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_DOCUMENTS)})) private List<DocumentAttachmentEntity> documents;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_NOTIFICATION_FEE_POLICY)})) private NotificationFeePolicy notificationFeePolicy;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PHYSICAL_COMMUNICATION_TYPE)})) private FullSentNotification.PhysicalCommunicationTypeEnum physicalCommunicationType;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_GROUP)})) private String group;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SENDER_DENOMINATION)})) private String senderDenomination;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SENDER_TAX_ID)})) private String senderTaxId;

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_AMOUNT)})) private Integer amount;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_PAYMENT_EXPIRATION_DATE)})) private String paymentExpirationDate;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_REQUEST_ID)})) private String requestId;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_TAXONOMY_CODE)})) private String taxonomyCode;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SOURCE_CHANNEL)})) private String sourceChannel;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_SOURCE_CHANNEL_DETAILS)})) private String sourceChannelDetails;
    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_VERSION)})) private int version;

}

package it.pagopa.pn.delivery.middleware.notificationdao.entities;


import it.pagopa.pn.api.dto.notification.NotificationPaymentInfoFeePolicies;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.FullSentNotification;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationEntity {

    public static final String FIELD_IUN = "iun";
    public static final String FIELD_PA_NOTIFICATION_ID = "paNotificationId";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_SENT_AT = "sentAt";
    public static final String FIELD_CANCELLED_IUN = "cancelledIun";
    public static final String FIELD_CANCELLED_BY_IUN = "cancelledByIun";
    public static final String FIELD_SENDER_PA_ID = "senderPaId";
    public static final String FIELD_RECIPIENTS_ORDER = "recipientsOrder";
    public static final String FIELD_RECIPIENTS_JSON = "recipientsJson";
    public static final String FIELD_DOCUMENTS_KEYS = "documentsKeys";
    public static final String FIELD_DOCUMENTS_DIGESTS_SHA_256 = "documentsDigestsSha256";
    public static final String FIELD_DOCUMENTS_VERSION_IDS = "documentsVersionIds";
    public static final String FIELD_DOCUMENTS_CONTENT_TYPES = "documentsContentTypes";
    public static final String FIELD_DOCUMENTS_TITLES = "documentsTitles";
    public static final String FIELD_IUV = "iuv";
    public static final String FIELD_NOTIFICATION_FEE_POLICY = "notificationFeePolicy";
    public static final String FIELD_F24_FLAT_RATE_KEY = "f24FlatRateKey";
    public static final String FIELD_F24_FLAT_RATE_DIGEST_SHA256 = "f24FlatRateDigestSha256";
    public static final String FIELD_F24_FLAT_RATE_VERSION_ID = "f24FlatRateVersionId";
    public static final String FIELD_F24_DIGITAL_KEY = "f24DigitalKey";
    public static final String FIELD_F24_DIGITAL_DIGEST_SHA256 = "f24DigitalDigestSha256";
    public static final String FIELD_F24_DIGITAL_VERSION_ID = "f24DigitalVersionId";
    public static final String FIELD_F24_ANALOG_KEY = "f24AnalogKey";
    public static final String FIELD_F24_ANALOG_DIGEST_SHA256 = "f24AnalogDigestSha256";
    public static final String FIELD_F24_ANALOG_VERSION_ID = "f24AnalogVersionId";
    public static final String FIELD_PHYSICAL_COMMUNICATION_TYPE = "physicalCommunicationType";
    public static final String FIELD_GROUP = "group";

    private String iun;
    private String paNotificationId;
    private String subject;
    private Instant sentAt;
    private String cancelledIun;
    private String cancelledByIun;
    private String senderPaId;
    private List<String> recipientsOrder;
    private Map<String,String> recipientsJson;
    private List<String> documentsKeys;
    private List<String> documentsDigestsSha256;
    private List<String> documentsVersionIds;
    private List<String> documentsContentTypes;
    private List<String> documentsTitles;
    private String iuv;
    private NotificationPaymentInfo.NotificationFeePolicyEnum notificationFeePolicy;
    private String f24FlatRateKey;
    private String f24FlatRateDigestSha256;
    private String f24FlatRateVersionId;
    private String f24DigitalKey;
    private String f24DigitalDigestSha256;
    private String f24DigitalVersionId;
    private String f24AnalogKey;
    private String f24AnalogDigestSha256;
    private String f24AnalogVersionId;
    private FullSentNotification.PhysicalCommunicationTypeEnum physicalCommunicationType;
    private String group;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_IUN)
    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    @DynamoDbAttribute(value = FIELD_PA_NOTIFICATION_ID)
    public String getPaNotificationId() {
        return paNotificationId;
    }

    public void setPaNotificationId(String paNotificationId) {
        this.paNotificationId = paNotificationId;
    }

    @DynamoDbAttribute(value = FIELD_SUBJECT)
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @DynamoDbAttribute(value = FIELD_SENT_AT)
    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @DynamoDbAttribute(value = FIELD_CANCELLED_IUN)
    public String getCancelledIun() {
        return cancelledIun;
    }

    public void setCancelledIun(String cancelledIun) {
        this.cancelledIun = cancelledIun;
    }

    @DynamoDbAttribute(value = FIELD_CANCELLED_BY_IUN)
    public String getCancelledByIun() {
        return cancelledByIun;
    }

    public void setCancelledByIun(String cancelledByIun) {
        this.cancelledByIun = cancelledByIun;
    }

    @DynamoDbAttribute(value = FIELD_SENDER_PA_ID)
    public String getSenderPaId() {
        return senderPaId;
    }

    public void setSenderPaId(String senderPaId) {
        this.senderPaId = senderPaId;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENTS_ORDER)
    public List<String> getRecipientsOrder() {
        return recipientsOrder;
    }

    public void setRecipientsOrder(List<String> recipientsOrder) {
        this.recipientsOrder = recipientsOrder;
    }

    @DynamoDbAttribute(value = FIELD_RECIPIENTS_JSON)
    public Map<String, String> getRecipientsJson() {
        return recipientsJson;
    }

    public void setRecipientsJson(Map<String, String> recipientsJson) {
        this.recipientsJson = recipientsJson;
    }

    @DynamoDbAttribute(value = FIELD_DOCUMENTS_KEYS)
    public List<String> getDocumentsKeys() {
        return documentsKeys;
    }

    public void setDocumentsKeys(List<String> documentsKeys) {
        this.documentsKeys = documentsKeys;
    }

    @DynamoDbAttribute(value = FIELD_DOCUMENTS_DIGESTS_SHA_256)
    public List<String> getDocumentsDigestsSha256() {
        return documentsDigestsSha256;
    }

    public void setDocumentsDigestsSha256(List<String> documentsDigestsSha256) {
        this.documentsDigestsSha256 = documentsDigestsSha256;
    }

    @DynamoDbAttribute(value = FIELD_DOCUMENTS_VERSION_IDS)
    public List<String> getDocumentsVersionIds() {
        return documentsVersionIds;
    }

    public void setDocumentsVersionIds(List<String> documentsVersionIds) {
        this.documentsVersionIds = documentsVersionIds;
    }

    @DynamoDbAttribute(value = FIELD_DOCUMENTS_CONTENT_TYPES)
    public List<String> getDocumentsContentTypes() {
        return documentsContentTypes;
    }

    public void setDocumentsContentTypes(List<String> documentsContentTypes) {
        this.documentsContentTypes = documentsContentTypes;
    }

    @DynamoDbAttribute(value = FIELD_DOCUMENTS_TITLES)
    public List<String> getDocumentsTitles() {
        return documentsTitles;
    }

    public void setDocumentsTitles(List<String> documentsTitles) {
        this.documentsTitles = documentsTitles;
    }

    @DynamoDbAttribute(value = FIELD_IUV)
    public String getIuv() {
        return iuv;
    }

    public void setIuv(String iuv) {
        this.iuv = iuv;
    }

    @DynamoDbAttribute(value = FIELD_NOTIFICATION_FEE_POLICY)
    public NotificationPaymentInfo.NotificationFeePolicyEnum getNotificationFeePolicy() {
        return notificationFeePolicy;
    }

    public void setNotificationFeePolicy(NotificationPaymentInfo.NotificationFeePolicyEnum notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
    }

    @DynamoDbAttribute(value = FIELD_F24_FLAT_RATE_KEY)
    public String getF24FlatRateKey() {
        return f24FlatRateKey;
    }

    public void setF24FlatRateKey(String f24FlatRateKey) {
        this.f24FlatRateKey = f24FlatRateKey;
    }

    @DynamoDbAttribute(value = FIELD_F24_FLAT_RATE_DIGEST_SHA256)
    public String getF24FlatRateDigestSha256() {
        return f24FlatRateDigestSha256;
    }

    public void setF24FlatRateDigestSha256(String f24FlatRateDigestSha256) {
        this.f24FlatRateDigestSha256 = f24FlatRateDigestSha256;
    }

    @DynamoDbAttribute(value = FIELD_F24_FLAT_RATE_VERSION_ID)
    public String getF24FlatRateVersionId() {
        return f24FlatRateVersionId;
    }

    public void setF24FlatRateVersionId(String f24FlatRateVersionId) {
        this.f24FlatRateVersionId = f24FlatRateVersionId;
    }

    @DynamoDbAttribute(value = FIELD_F24_DIGITAL_KEY)
    public String getF24DigitalKey() {
        return f24DigitalKey;
    }

    public void setF24DigitalKey(String f24DigitalKey) {
        this.f24DigitalKey = f24DigitalKey;
    }

    @DynamoDbAttribute(value = FIELD_F24_DIGITAL_DIGEST_SHA256)
    public String getF24DigitalDigestSha256() {
        return f24DigitalDigestSha256;
    }

    public void setF24DigitalDigestSha256(String f24DigitalDigestSha256) {
        this.f24DigitalDigestSha256 = f24DigitalDigestSha256;
    }

    @DynamoDbAttribute(value = FIELD_F24_DIGITAL_VERSION_ID)
    public String getF24DigitalVersionId() {
        return f24DigitalVersionId;
    }

    public void setF24DigitalVersionId(String f24DigitalVersionId) {
        this.f24DigitalVersionId = f24DigitalVersionId;
    }

    @DynamoDbAttribute(value = FIELD_F24_ANALOG_KEY)
    public String getF24AnalogKey() {
        return f24AnalogKey;
    }

    public void setF24AnalogKey(String f24AnalogKey) {
        this.f24AnalogKey = f24AnalogKey;
    }

    @DynamoDbAttribute(value = FIELD_F24_ANALOG_DIGEST_SHA256)
    public String getF24AnalogDigestSha256() {
        return f24AnalogDigestSha256;
    }

    public void setF24AnalogDigestSha256(String f24AnalogDigestSha256) {
        this.f24AnalogDigestSha256 = f24AnalogDigestSha256;
    }

    @DynamoDbAttribute(value = FIELD_F24_ANALOG_VERSION_ID)
    public String getF24AnalogVersionId() {
        return f24AnalogVersionId;
    }

    public void setF24AnalogVersionId(String f24AnalogVersionId) {
        this.f24AnalogVersionId = f24AnalogVersionId;
    }

    @DynamoDbAttribute(value = FIELD_PHYSICAL_COMMUNICATION_TYPE)
    public FullSentNotification.PhysicalCommunicationTypeEnum getPhysicalCommunicationType() {
        return physicalCommunicationType;
    }

    public void setPhysicalCommunicationType(FullSentNotification.PhysicalCommunicationTypeEnum physicalCommunicationType) {
        this.physicalCommunicationType = physicalCommunicationType;
    }

    @DynamoDbAttribute(value = FIELD_GROUP)
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}

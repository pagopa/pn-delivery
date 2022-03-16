package it.pagopa.pn.delivery.middleware.model.notification;


import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.NotificationPaymentInfoFeePolicies;
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

    public static final String NOTIFICATIONS_TABLE_NAME = "Notifications";

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
    private NotificationPaymentInfoFeePolicies notificationFeePolicy;
    private String f24FlatRateKey;
    private String f24FlatRateDigestSha256;
    private String f24FlatRateVersionId;
    private String f24DigitalKey;
    private String f24DigitalDigestSha256;
    private String f24DigitalVersionId;
    private String f24AnalogKey;
    private String f24AnalogDigestSha256;
    private String f24AnalogVersionId;
    private ServiceLevelType physicalCommunicationType;

    @DynamoDbPartitionKey
    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    @DynamoDbAttribute(value = "paNotificationId")
    public String getPaNotificationId() {
        return paNotificationId;
    }

    public void setPaNotificationId(String paNotificationId) {
        this.paNotificationId = paNotificationId;
    }

    @DynamoDbAttribute(value = "subject")
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @DynamoDbAttribute(value = "sentAt")
    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    @DynamoDbAttribute(value = "cancelledIun")
    public String getCancelledIun() {
        return cancelledIun;
    }

    public void setCancelledIun(String cancelledIun) {
        this.cancelledIun = cancelledIun;
    }

    @DynamoDbAttribute(value = "cancelledByIun")
    public String getCancelledByIun() {
        return cancelledByIun;
    }

    public void setCancelledByIun(String cancelledByIun) {
        this.cancelledByIun = cancelledByIun;
    }

    @DynamoDbAttribute(value = "senderPaId")
    public String getSenderPaId() {
        return senderPaId;
    }

    public void setSenderPaId(String senderPaId) {
        this.senderPaId = senderPaId;
    }

    @DynamoDbAttribute(value = "recipientsOrder")
    public List<String> getRecipientsOrder() {
        return recipientsOrder;
    }

    public void setRecipientsOrder(List<String> recipientsOrder) {
        this.recipientsOrder = recipientsOrder;
    }

    @DynamoDbAttribute(value = "recipientsJson")
    public Map<String, String> getRecipientsJson() {
        return recipientsJson;
    }

    public void setRecipientsJson(Map<String, String> recipientsJson) {
        this.recipientsJson = recipientsJson;
    }

    @DynamoDbAttribute(value = "documentsKeys")
    public List<String> getDocumentsKeys() {
        return documentsKeys;
    }

    public void setDocumentsKeys(List<String> documentsKeys) {
        this.documentsKeys = documentsKeys;
    }

    @DynamoDbAttribute(value = "documentsDigestsSha256")
    public List<String> getDocumentsDigestsSha256() {
        return documentsDigestsSha256;
    }

    public void setDocumentsDigestsSha256(List<String> documentsDigestsSha256) {
        this.documentsDigestsSha256 = documentsDigestsSha256;
    }

    @DynamoDbAttribute(value = "documentsVersionIds")
    public List<String> getDocumentsVersionIds() {
        return documentsVersionIds;
    }

    public void setDocumentsVersionIds(List<String> documentsVersionIds) {
        this.documentsVersionIds = documentsVersionIds;
    }

    @DynamoDbAttribute(value = "documentsContentTypes")
    public List<String> getDocumentsContentTypes() {
        return documentsContentTypes;
    }

    public void setDocumentsContentTypes(List<String> documentsContentTypes) {
        this.documentsContentTypes = documentsContentTypes;
    }

    @DynamoDbAttribute(value = "documentsTitles")
    public List<String> getDocumentsTitles() {
        return documentsTitles;
    }

    public void setDocumentsTitles(List<String> documentsTitles) {
        this.documentsTitles = documentsTitles;
    }

    @DynamoDbAttribute(value = "iuv")
    public String getIuv() {
        return iuv;
    }

    public void setIuv(String iuv) {
        this.iuv = iuv;
    }

    @DynamoDbAttribute(value = "notificationFeePolicy")
    public NotificationPaymentInfoFeePolicies getNotificationFeePolicy() {
        return notificationFeePolicy;
    }

    public void setNotificationFeePolicy(NotificationPaymentInfoFeePolicies notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
    }

    @DynamoDbAttribute(value = "f24FlatRateKey")
    public String getF24FlatRateKey() {
        return f24FlatRateKey;
    }

    public void setF24FlatRateKey(String f24FlatRateKey) {
        this.f24FlatRateKey = f24FlatRateKey;
    }

    @DynamoDbAttribute(value = "f24FlatRateDigestSha256")
    public String getF24FlatRateDigestSha256() {
        return f24FlatRateDigestSha256;
    }

    public void setF24FlatRateDigestSha256(String f24FlatRateDigestSha256) {
        this.f24FlatRateDigestSha256 = f24FlatRateDigestSha256;
    }

    @DynamoDbAttribute(value = "f24FlatRateVersionId")
    public String getF24FlatRateVersionId() {
        return f24FlatRateVersionId;
    }

    public void setF24FlatRateVersionId(String f24FlatRateVersionId) {
        this.f24FlatRateVersionId = f24FlatRateVersionId;
    }

    @DynamoDbAttribute(value = "f24DigitalKey")
    public String getF24DigitalKey() {
        return f24DigitalKey;
    }

    public void setF24DigitalKey(String f24DigitalKey) {
        this.f24DigitalKey = f24DigitalKey;
    }

    @DynamoDbAttribute(value = "f24DigitalDigestSha256")
    public String getF24DigitalDigestSha256() {
        return f24DigitalDigestSha256;
    }

    public void setF24DigitalDigestSha256(String f24DigitalDigestSha256) {
        this.f24DigitalDigestSha256 = f24DigitalDigestSha256;
    }

    @DynamoDbAttribute(value = "f24DigitalVersionId")
    public String getF24DigitalVersionId() {
        return f24DigitalVersionId;
    }

    public void setF24DigitalVersionId(String f24DigitalVersionId) {
        this.f24DigitalVersionId = f24DigitalVersionId;
    }

    @DynamoDbAttribute(value = "f24AnalogKey")
    public String getF24AnalogKey() {
        return f24AnalogKey;
    }

    public void setF24AnalogKey(String f24AnalogKey) {
        this.f24AnalogKey = f24AnalogKey;
    }

    @DynamoDbAttribute(value = "f24AnalogDigestSha256")
    public String getF24AnalogDigestSha256() {
        return f24AnalogDigestSha256;
    }

    public void setF24AnalogDigestSha256(String f24AnalogDigestSha256) {
        this.f24AnalogDigestSha256 = f24AnalogDigestSha256;
    }

    @DynamoDbAttribute(value = "f24AnalogVersionId")
    public String getF24AnalogVersionId() {
        return f24AnalogVersionId;
    }

    public void setF24AnalogVersionId(String f24AnalogVersionId) {
        this.f24AnalogVersionId = f24AnalogVersionId;
    }

    @DynamoDbAttribute(value = "physicalCommunicationType")
    public ServiceLevelType getPhysicalCommunicationType() {
        return physicalCommunicationType;
    }

    public void setPhysicalCommunicationType(ServiceLevelType physicalCommunicationType) {
        this.physicalCommunicationType = physicalCommunicationType;
    }
}

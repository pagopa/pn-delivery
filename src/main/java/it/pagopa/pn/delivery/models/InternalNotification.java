package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InternalNotification {

    private String idempotenceToken;
    private String paProtocolNumber;
    private String subject;

    private String _abstract;
    private List<NotificationRecipient> recipients;
    private List<NotificationDocument> documents;
    private NotificationFeePolicy notificationFeePolicy;
    private String cancelledIun;
    private FullSentNotificationV25.PhysicalCommunicationTypeEnum physicalCommunicationType;
    private String senderDenomination;
    private String senderTaxId;
    private String group;
    private Integer amount;
    private String paymentExpirationDate;
    private String taxonomyCode;
    private Integer paFee;
    private Integer vat;
    private String senderPaId;
    private String iun;
    private OffsetDateTime sentAt;
    private String cancelledByIun;
    private Boolean documentsAvailable;
    private NotificationStatus notificationStatus;
    private List<NotificationStatusHistoryElement> notificationStatusHistory;
    private List<TimelineElementV25> timeline;
    private List<String> recipientIds;
    private String sourceChannel;
    private String sourceChannelDetails;
    private NewNotificationRequestV24.PagoPaIntModeEnum pagoPaIntMode;
    private String version;
    private List<String> additionalLanguages;

    public InternalNotification idempotenceToken(String idempotenceToken) {
        this.idempotenceToken = idempotenceToken;
        return this;
    }

    public String getIdempotenceToken() {
        return idempotenceToken;
    }

    public void setIdempotenceToken(String idempotenceToken) {
        this.idempotenceToken = idempotenceToken;
    }

    public InternalNotification paProtocolNumber(String paProtocolNumber) {
        this.paProtocolNumber = paProtocolNumber;
        return this;
    }

    public String getPaProtocolNumber() {
        return paProtocolNumber;
    }

    public void setPaProtocolNumber(String paProtocolNumber) {
        this.paProtocolNumber = paProtocolNumber;
    }

    public InternalNotification subject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public InternalNotification _abstract(String _abstract) {
        this._abstract = _abstract;
        return this;
    }

    public String getAbstract() {
        return _abstract;
    }

    public void setAbstract(String _abstract) {
        this._abstract = _abstract;
    }

    public InternalNotification recipients(List<NotificationRecipient> recipients) {
        this.recipients = recipients;
        return this;
    }

    public InternalNotification addRecipientsItem(NotificationRecipient recipientsItem) {
        if (this.recipients == null) {
            this.recipients = new ArrayList<>();
        }
        this.recipients.add(recipientsItem);
        return this;
    }

    public List<NotificationRecipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<NotificationRecipient> recipients) {
        this.recipients = recipients;
    }

    public InternalNotification documents(List<NotificationDocument> documents) {
        this.documents = documents;
        return this;
    }

    public InternalNotification addDocumentsItem(NotificationDocument documentsItem) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(documentsItem);
        return this;
    }

    public List<NotificationDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<NotificationDocument> documents) {
        this.documents = documents;
    }

    public InternalNotification notificationFeePolicy(NotificationFeePolicy notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
        return this;
    }

    public NotificationFeePolicy getNotificationFeePolicy() {
        return notificationFeePolicy;
    }

    public void setNotificationFeePolicy(NotificationFeePolicy notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
    }

    public InternalNotification cancelledIun(String cancelledIun) {
        this.cancelledIun = cancelledIun;
        return this;
    }

    public String getCancelledIun() {
        return cancelledIun;
    }

    public void setCancelledIun(String cancelledIun) {
        this.cancelledIun = cancelledIun;
    }

    public InternalNotification physicalCommunicationType(FullSentNotificationV25.PhysicalCommunicationTypeEnum physicalCommunicationType) {
        this.physicalCommunicationType = physicalCommunicationType;
        return this;
    }

    public FullSentNotificationV25.PhysicalCommunicationTypeEnum getPhysicalCommunicationType() {
        return physicalCommunicationType;
    }

    public void setPhysicalCommunicationType(FullSentNotificationV25.PhysicalCommunicationTypeEnum physicalCommunicationType) {
        this.physicalCommunicationType = physicalCommunicationType;
    }

    public InternalNotification senderDenomination(String senderDenomination) {
        this.senderDenomination = senderDenomination;
        return this;
    }

    public String getSenderDenomination() {
        return senderDenomination;
    }

    public void setSenderDenomination(String senderDenomination) {
        this.senderDenomination = senderDenomination;
    }

    public InternalNotification senderTaxId(String senderTaxId) {
        this.senderTaxId = senderTaxId;
        return this;
    }

    public String getSenderTaxId() {
        return senderTaxId;
    }

    public void setSenderTaxId(String senderTaxId) {
        this.senderTaxId = senderTaxId;
    }

    public InternalNotification group(String group) {
        this.group = group;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public InternalNotification amount(Integer amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Importo della notifica in eurocent
     *
     * @return amount
     */

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public InternalNotification paymentExpirationDate(String paymentExpirationDate) {
        this.paymentExpirationDate = paymentExpirationDate;
        return this;
    }

    public String getPaymentExpirationDate() {
        return paymentExpirationDate;
    }

    public void setPaymentExpirationDate(String paymentExpirationDate) {
        this.paymentExpirationDate = paymentExpirationDate;
    }

    public InternalNotification taxonomyCode(String taxonomyCode) {
        this.taxonomyCode = taxonomyCode;
        return this;
    }

    public String getTaxonomyCode() {
        return taxonomyCode;
    }

    public void setTaxonomyCode(String taxonomyCode) {
        this.taxonomyCode = taxonomyCode;
    }

    public InternalNotification senderPaId(String senderPaId) {
        this.senderPaId = senderPaId;
        return this;
    }

    public String getSenderPaId() {
        return senderPaId;
    }

    public void setSenderPaId(String senderPaId) {
        this.senderPaId = senderPaId;
    }

    public InternalNotification iun(String iun) {
        this.iun = iun;
        return this;
    }

    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    public InternalNotification sentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public InternalNotification cancelledByIun(String cancelledByIun) {
        this.cancelledByIun = cancelledByIun;
        return this;
    }

    public String getCancelledByIun() {
        return cancelledByIun;
    }

    public void setCancelledByIun(String cancelledByIun) {
        this.cancelledByIun = cancelledByIun;
    }

    public InternalNotification documentsAvailable(Boolean documentsAvailable) {
        this.documentsAvailable = documentsAvailable;
        return this;
    }

    /**
     * Indica se i documenti notificati sono ancora disponibili.
     *
     * @return documentsAvailable
     */

    public Boolean getDocumentsAvailable() {
        return documentsAvailable;
    }

    public void setDocumentsAvailable(Boolean documentsAvailable) {
        this.documentsAvailable = documentsAvailable;
    }

    public InternalNotification notificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
        return this;
    }

    /**
     * Get notificationStatus
     *
     * @return notificationStatus
     */
    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public InternalNotification notificationStatusHistory(List<NotificationStatusHistoryElement> notificationStatusHistory) {
        this.notificationStatusHistory = notificationStatusHistory;
        return this;
    }

    public InternalNotification addNotificationStatusHistoryItem(NotificationStatusHistoryElement notificationStatusHistoryItem) {
        if (this.notificationStatusHistory == null) {
            this.notificationStatusHistory = new ArrayList<>();
        }
        this.notificationStatusHistory.add(notificationStatusHistoryItem);
        return this;
    }

    /**
     * elenco degli avanzamenti effettuati dal processo di notifica
     *
     * @return notificationStatusHistory
     */
    public List<NotificationStatusHistoryElement> getNotificationStatusHistory() {
        return notificationStatusHistory;
    }

    public void setNotificationStatusHistory(List<NotificationStatusHistoryElement> notificationStatusHistory) {
        this.notificationStatusHistory = notificationStatusHistory;
    }

    public InternalNotification timeline(List<TimelineElementV25> timeline) {
        this.timeline = timeline;
        return this;
    }

    public InternalNotification addTimelineItem(TimelineElementV25 timelineItem) {
        if (this.timeline == null) {
            this.timeline = new ArrayList<>();
        }
        this.timeline.add(timelineItem);
        return this;
    }

    public List<TimelineElementV25> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<TimelineElementV25> timeline) {
        this.timeline = timeline;
    }

    public InternalNotification recipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
        return this;
    }

    public InternalNotification addRecipientIdsItem(String recipientIdsItem) {
        if (this.recipientIds == null) {
            this.recipientIds = new ArrayList<>();
        }
        this.recipientIds.add(recipientIdsItem);
        return this;
    }

    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

    public InternalNotification sourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
        return this;
    }

    public String getVersion() { return version; }

    public void setVersion(String version) { this.version = version; }



}
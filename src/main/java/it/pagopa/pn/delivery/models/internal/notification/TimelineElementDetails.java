package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TimelineElementDetails {

    private String legalFactId;
    private Integer recIndex;
    private NotificationPhysicalAddress oldAddress;
    private NotificationPhysicalAddress normalizedAddress;
    private String generatedAarUrl;
    private NotificationPhysicalAddress physicalAddress;
    private String legalfactId;
    private EndWorkflowStatus endWorkflowStatus;
    private OffsetDateTime completionWorkflowDate;
    private OffsetDateTime legalFactGenerationDate;
    private NotificationDigitalAddress digitalAddress;
    private DigitalAddressSource digitalAddressSource;
    private Boolean isAvailable;
    private OffsetDateTime attemptDate;
    private OffsetDateTime eventTimestamp;
    private String raddType;
    private String raddTransactionId;
    private DelegateInfo delegateInfo;
    private Long notificationCost;
    private DeliveryMode deliveryMode;
    private ContactPhase contactPhase;
    private Integer sentAttemptMade;
    private OffsetDateTime sendDate;
    private List<NotificationRefusedError> refusalReasons = null;
    private OffsetDateTime lastAttemptDate;
    private OffsetDateTime schedulingDate;
    private IoSendMessageResult ioSendMessageResult;
    private Integer retryNumber;
    private DigitalAddressSource nextDigitalAddressSource;
    private Integer nextSourceAttemptsMade;
    private OffsetDateTime nextLastAttemptMadeForSource;
    private ResponseStatus responseStatus;
    private OffsetDateTime notificationDate;
    private String deliveryFailureCause;
    private String deliveryDetailCode;
    private List<SendingReceipt> sendingReceipts = null;
    private Boolean shouldRetry;
    private ServiceLevel serviceLevel;
    private String relatedRequestId;
    private String productType;
    private Integer analogCost;
    private Integer numberOfPages;
    private Integer envelopeWeight;
    private String prepareRequestId;
    private NotificationPhysicalAddress newAddress;
    private List<AttachmentDetails> attachments = null;
    private String sendRequestId;
    private String registeredLetterCode;
    private String aarKey;
    private String reasonCode;
    private String reason;
    private NotificationRecipientV21.RecipientTypeEnum recipientType;
    private Integer amount;
    private String creditorTaxId;
    private String noticeCode;
    private String idF24;
    private String paymentSourceChannel;
    private Boolean uncertainPaymentDate;
    private OffsetDateTime schedulingAnalogDate;

    public TimelineElementDetails legalFactId(String legalFactId) {
        this.legalFactId = legalFactId;
        return this;
    }

    public String getLegalFactId() {
        return legalFactId;
    }

    public void setLegalFactId(String legalFactId) {
        this.legalFactId = legalFactId;
    }

    public TimelineElementDetails recIndex(Integer recIndex) {
        this.recIndex = recIndex;
        return this;
    }

    public Integer getRecIndex() {
        return recIndex;
    }

    public void setRecIndex(Integer recIndex) {
        this.recIndex = recIndex;
    }

    public TimelineElementDetails oldAddress(NotificationPhysicalAddress oldAddress) {
        this.oldAddress = oldAddress;
        return this;
    }

    public NotificationPhysicalAddress getOldAddress() {
        return oldAddress;
    }

    public void setOldAddress(NotificationPhysicalAddress oldAddress) {
        this.oldAddress = oldAddress;
    }

    public TimelineElementDetails normalizedAddress(NotificationPhysicalAddress normalizedAddress) {
        this.normalizedAddress = normalizedAddress;
        return this;
    }

    public NotificationPhysicalAddress getNormalizedAddress() {
        return normalizedAddress;
    }

    public void setNormalizedAddress(NotificationPhysicalAddress normalizedAddress) {
        this.normalizedAddress = normalizedAddress;
    }

    public TimelineElementDetails generatedAarUrl(String generatedAarUrl) {
        this.generatedAarUrl = generatedAarUrl;
        return this;
    }

    public String getGeneratedAarUrl() {
        return generatedAarUrl;
    }

    public void setGeneratedAarUrl(String generatedAarUrl) {
        this.generatedAarUrl = generatedAarUrl;
    }

    public TimelineElementDetails physicalAddress(NotificationPhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
        return this;
    }

    public NotificationPhysicalAddress getNotificationPhysicalAddress() {
        return physicalAddress;
    }

    public void setNotificationPhysicalAddress(NotificationPhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    public TimelineElementDetails legalfactId(String legalfactId) {
        this.legalfactId = legalfactId;
        return this;
    }

    public String getLegalfactId() {
        return legalfactId;
    }

    public void setLegalfactId(String legalfactId) {
        this.legalfactId = legalfactId;
    }

    public TimelineElementDetails endWorkflowStatus(EndWorkflowStatus endWorkflowStatus) {
        this.endWorkflowStatus = endWorkflowStatus;
        return this;
    }

    public EndWorkflowStatus getEndWorkflowStatus() {
        return endWorkflowStatus;
    }

    public void setEndWorkflowStatus(EndWorkflowStatus endWorkflowStatus) {
        this.endWorkflowStatus = endWorkflowStatus;
    }

    public TimelineElementDetails completionWorkflowDate(OffsetDateTime completionWorkflowDate) {
        this.completionWorkflowDate = completionWorkflowDate;
        return this;
    }

    public OffsetDateTime getCompletionWorkflowDate() {
        return completionWorkflowDate;
    }

    public void setCompletionWorkflowDate(OffsetDateTime completionWorkflowDate) {
        this.completionWorkflowDate = completionWorkflowDate;
    }

    public TimelineElementDetails legalFactGenerationDate(OffsetDateTime legalFactGenerationDate) {
        this.legalFactGenerationDate = legalFactGenerationDate;
        return this;
    }

    public OffsetDateTime getLegalFactGenerationDate() {
        return legalFactGenerationDate;
    }

    public void setLegalFactGenerationDate(OffsetDateTime legalFactGenerationDate) {
        this.legalFactGenerationDate = legalFactGenerationDate;
    }

    public TimelineElementDetails digitalAddress(NotificationDigitalAddress digitalAddress) {
        this.digitalAddress = digitalAddress;
        return this;
    }

    public NotificationDigitalAddress getDigitalAddress() {
        return digitalAddress;
    }

    public void setDigitalAddress(NotificationDigitalAddress digitalAddress) {
        this.digitalAddress = digitalAddress;
    }

    public TimelineElementDetails digitalAddressSource(DigitalAddressSource digitalAddressSource) {
        this.digitalAddressSource = digitalAddressSource;
        return this;
    }

    public DigitalAddressSource getDigitalAddressSource() {
        return digitalAddressSource;
    }

    public void setDigitalAddressSource(DigitalAddressSource digitalAddressSource) {
        this.digitalAddressSource = digitalAddressSource;
    }

    public TimelineElementDetails isAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
        return this;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public TimelineElementDetails attemptDate(OffsetDateTime attemptDate) {
        this.attemptDate = attemptDate;
        return this;
    }

    public OffsetDateTime getAttemptDate() {
        return attemptDate;
    }

    public void setAttemptDate(OffsetDateTime attemptDate) {
        this.attemptDate = attemptDate;
    }

    public TimelineElementDetails eventTimestamp(OffsetDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
        return this;
    }

    public OffsetDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(OffsetDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public TimelineElementDetails raddType(String raddType) {
        this.raddType = raddType;
        return this;
    }

    public String getRaddType() {
        return raddType;
    }

    public void setRaddType(String raddType) {
        this.raddType = raddType;
    }

    public TimelineElementDetails raddTransactionId(String raddTransactionId) {
        this.raddTransactionId = raddTransactionId;
        return this;
    }

    public String getRaddTransactionId() {
        return raddTransactionId;
    }

    public void setRaddTransactionId(String raddTransactionId) {
        this.raddTransactionId = raddTransactionId;
    }

    public TimelineElementDetails delegateInfo(DelegateInfo delegateInfo) {
        this.delegateInfo = delegateInfo;
        return this;
    }

    public DelegateInfo getDelegateInfo() {
        return delegateInfo;
    }

    public void setDelegateInfo(DelegateInfo delegateInfo) {
        this.delegateInfo = delegateInfo;
    }

    public TimelineElementDetails notificationCost(Long notificationCost) {
        this.notificationCost = notificationCost;
        return this;
    }

    public Long getNotificationCost() {
        return notificationCost;
    }

    public void setNotificationCost(Long notificationCost) {
        this.notificationCost = notificationCost;
    }

    public TimelineElementDetails deliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
        return this;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public TimelineElementDetails contactPhase(ContactPhase contactPhase) {
        this.contactPhase = contactPhase;
        return this;
    }

    public ContactPhase getContactPhase() {
        return contactPhase;
    }

    public void setContactPhase(ContactPhase contactPhase) {
        this.contactPhase = contactPhase;
    }

    public TimelineElementDetails sentAttemptMade(Integer sentAttemptMade) {
        this.sentAttemptMade = sentAttemptMade;
        return this;
    }

    public Integer getSentAttemptMade() {
        return sentAttemptMade;
    }

    public void setSentAttemptMade(Integer sentAttemptMade) {
        this.sentAttemptMade = sentAttemptMade;
    }

    public TimelineElementDetails sendDate(OffsetDateTime sendDate) {
        this.sendDate = sendDate;
        return this;
    }

    public OffsetDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(OffsetDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public TimelineElementDetails refusalReasons(List<NotificationRefusedError> refusalReasons) {
        this.refusalReasons = refusalReasons;
        return this;
    }

    public TimelineElementDetails addRefusalReasonsItem(NotificationRefusedError refusalReasonsItem) {
        if (this.refusalReasons == null) {
            this.refusalReasons = new ArrayList<>();
        }
        this.refusalReasons.add(refusalReasonsItem);
        return this;
    }

    public List<NotificationRefusedError> getRefusalReasons() {
        return refusalReasons;
    }

    public void setRefusalReasons(List<NotificationRefusedError> refusalReasons) {
        this.refusalReasons = refusalReasons;
    }

    public TimelineElementDetails lastAttemptDate(OffsetDateTime lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
        return this;
    }

    public OffsetDateTime getLastAttemptDate() {
        return lastAttemptDate;
    }

    public void setLastAttemptDate(OffsetDateTime lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
    }

    public TimelineElementDetails schedulingDate(OffsetDateTime schedulingDate) {
        this.schedulingDate = schedulingDate;
        return this;
    }

    public OffsetDateTime getSchedulingDate() {
        return schedulingDate;
    }

    public void setSchedulingDate(OffsetDateTime schedulingDate) {
        this.schedulingDate = schedulingDate;
    }

    public TimelineElementDetails ioSendMessageResult(IoSendMessageResult ioSendMessageResult) {
        this.ioSendMessageResult = ioSendMessageResult;
        return this;
    }

    public IoSendMessageResult getIoSendMessageResult() {
        return ioSendMessageResult;
    }

    public void setIoSendMessageResult(IoSendMessageResult ioSendMessageResult) {
        this.ioSendMessageResult = ioSendMessageResult;
    }

    public TimelineElementDetails retryNumber(Integer retryNumber) {
        this.retryNumber = retryNumber;
        return this;
    }

    public Integer getRetryNumber() {
        return retryNumber;
    }

    public void setRetryNumber(Integer retryNumber) {
        this.retryNumber = retryNumber;
    }

    public TimelineElementDetails nextDigitalAddressSource(DigitalAddressSource nextDigitalAddressSource) {
        this.nextDigitalAddressSource = nextDigitalAddressSource;
        return this;
    }

    public DigitalAddressSource getNextDigitalAddressSource() {
        return nextDigitalAddressSource;
    }

    public void setNextDigitalAddressSource(DigitalAddressSource nextDigitalAddressSource) {
        this.nextDigitalAddressSource = nextDigitalAddressSource;
    }

    public TimelineElementDetails nextSourceAttemptsMade(Integer nextSourceAttemptsMade) {
        this.nextSourceAttemptsMade = nextSourceAttemptsMade;
        return this;
    }

    public Integer getNextSourceAttemptsMade() {
        return nextSourceAttemptsMade;
    }

    public void setNextSourceAttemptsMade(Integer nextSourceAttemptsMade) {
        this.nextSourceAttemptsMade = nextSourceAttemptsMade;
    }

    public TimelineElementDetails nextLastAttemptMadeForSource(OffsetDateTime nextLastAttemptMadeForSource) {
        this.nextLastAttemptMadeForSource = nextLastAttemptMadeForSource;
        return this;
    }

    public OffsetDateTime getNextLastAttemptMadeForSource() {
        return nextLastAttemptMadeForSource;
    }

    public void setNextLastAttemptMadeForSource(OffsetDateTime nextLastAttemptMadeForSource) {
        this.nextLastAttemptMadeForSource = nextLastAttemptMadeForSource;
    }

    public TimelineElementDetails responseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public TimelineElementDetails notificationDate(OffsetDateTime notificationDate) {
        this.notificationDate = notificationDate;
        return this;
    }

    public OffsetDateTime getNotificationDate() {
        return notificationDate;
    }

    public void setNotificationDate(OffsetDateTime notificationDate) {
        this.notificationDate = notificationDate;
    }

    public TimelineElementDetails deliveryFailureCause(String deliveryFailureCause) {
        this.deliveryFailureCause = deliveryFailureCause;
        return this;
    }

    public String getDeliveryFailureCause() {
        return deliveryFailureCause;
    }

    public void setDeliveryFailureCause(String deliveryFailureCause) {
        this.deliveryFailureCause = deliveryFailureCause;
    }

    public TimelineElementDetails deliveryDetailCode(String deliveryDetailCode) {
        this.deliveryDetailCode = deliveryDetailCode;
        return this;
    }

    public String getDeliveryDetailCode() {
        return deliveryDetailCode;
    }

    public void setDeliveryDetailCode(String deliveryDetailCode) {
        this.deliveryDetailCode = deliveryDetailCode;
    }

    public TimelineElementDetails sendingReceipts(List<SendingReceipt> sendingReceipts) {
        this.sendingReceipts = sendingReceipts;
        return this;
    }

    public TimelineElementDetails addSendingReceiptsItem(SendingReceipt sendingReceiptsItem) {
        if (this.sendingReceipts == null) {
            this.sendingReceipts = new ArrayList<>();
        }
        this.sendingReceipts.add(sendingReceiptsItem);
        return this;
    }

    public List<SendingReceipt> getSendingReceipts() {
        return sendingReceipts;
    }

    public void setSendingReceipts(List<SendingReceipt> sendingReceipts) {
        this.sendingReceipts = sendingReceipts;
    }

    public TimelineElementDetails shouldRetry(Boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
        return this;
    }

    public Boolean getShouldRetry() {
        return shouldRetry;
    }

    public void setShouldRetry(Boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
    }

    public TimelineElementDetails serviceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
        return this;
    }

    public ServiceLevel getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(ServiceLevel serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public TimelineElementDetails relatedRequestId(String relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
        return this;
    }

    public String getRelatedRequestId() {
        return relatedRequestId;
    }

    public void setRelatedRequestId(String relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
    }

    public TimelineElementDetails productType(String productType) {
        this.productType = productType;
        return this;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public TimelineElementDetails analogCost(Integer analogCost) {
        this.analogCost = analogCost;
        return this;
    }

    public Integer getAnalogCost() {
        return analogCost;
    }

    public void setAnalogCost(Integer analogCost) {
        this.analogCost = analogCost;
    }

    public TimelineElementDetails numberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
        return this;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public TimelineElementDetails envelopeWeight(Integer envelopeWeight) {
        this.envelopeWeight = envelopeWeight;
        return this;
    }

    public Integer getEnvelopeWeight() {
        return envelopeWeight;
    }

    public void setEnvelopeWeight(Integer envelopeWeight) {
        this.envelopeWeight = envelopeWeight;
    }

    public TimelineElementDetails prepareRequestId(String prepareRequestId) {
        this.prepareRequestId = prepareRequestId;
        return this;
    }

    public String getPrepareRequestId() {
        return prepareRequestId;
    }

    public void setPrepareRequestId(String prepareRequestId) {
        this.prepareRequestId = prepareRequestId;
    }

    public TimelineElementDetails newAddress(NotificationPhysicalAddress newAddress) {
        this.newAddress = newAddress;
        return this;
    }

    public NotificationPhysicalAddress getNewAddress() {
        return newAddress;
    }

    public void setNewAddress(NotificationPhysicalAddress newAddress) {
        this.newAddress = newAddress;
    }

    public TimelineElementDetails attachments(List<AttachmentDetails> attachments) {
        this.attachments = attachments;
        return this;
    }

    public TimelineElementDetails addAttachmentsItem(AttachmentDetails attachmentsItem) {
        if (this.attachments == null) {
            this.attachments = new ArrayList<>();
        }
        this.attachments.add(attachmentsItem);
        return this;
    }

    public List<AttachmentDetails> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDetails> attachments) {
        this.attachments = attachments;
    }

    public TimelineElementDetails sendRequestId(String sendRequestId) {
        this.sendRequestId = sendRequestId;
        return this;
    }

    public String getSendRequestId() {
        return sendRequestId;
    }

    public void setSendRequestId(String sendRequestId) {
        this.sendRequestId = sendRequestId;
    }

    public TimelineElementDetails registeredLetterCode(String registeredLetterCode) {
        this.registeredLetterCode = registeredLetterCode;
        return this;
    }
    public String getRegisteredLetterCode() {
        return registeredLetterCode;
    }

    public void setRegisteredLetterCode(String registeredLetterCode) {
        this.registeredLetterCode = registeredLetterCode;
    }

    public TimelineElementDetails aarKey(String aarKey) {
        this.aarKey = aarKey;
        return this;
    }
    public String getAarKey() {
        return aarKey;
    }

    public void setAarKey(String aarKey) {
        this.aarKey = aarKey;
    }

    public TimelineElementDetails reasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
        return this;
    }
    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public TimelineElementDetails reason(String reason) {
        this.reason = reason;
        return this;
    }
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public TimelineElementDetails recipientType(NotificationRecipientV21.RecipientTypeEnum recipientType) {
        this.recipientType = recipientType;
        return this;
    }
    public NotificationRecipientV21.RecipientTypeEnum getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(NotificationRecipientV21.RecipientTypeEnum recipientType) {
        this.recipientType = recipientType;
    }

    public TimelineElementDetails amount(Integer amount) {
        this.amount = amount;
        return this;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public TimelineElementDetails creditorTaxId(String creditorTaxId) {
        this.creditorTaxId = creditorTaxId;
        return this;
    }
    public String getCreditorTaxId() {
        return creditorTaxId;
    }

    public void setCreditorTaxId(String creditorTaxId) {
        this.creditorTaxId = creditorTaxId;
    }

    public TimelineElementDetails noticeCode(String noticeCode) {
        this.noticeCode = noticeCode;
        return this;
    }
    public String getNoticeCode() {
        return noticeCode;
    }

    public void setNoticeCode(String noticeCode) {
        this.noticeCode = noticeCode;
    }

    public TimelineElementDetails idF24(String idF24) {
        this.idF24 = idF24;
        return this;
    }

    /**
     * un UUID che identifica un pagamento f24
     * @return idF24
     */

    public String getIdF24() {
        return idF24;
    }

    public void setIdF24(String idF24) {
        this.idF24 = idF24;
    }

    public TimelineElementDetails paymentSourceChannel(String paymentSourceChannel) {
        this.paymentSourceChannel = paymentSourceChannel;
        return this;
    }
    public String getPaymentSourceChannel() {
        return paymentSourceChannel;
    }

    public void setPaymentSourceChannel(String paymentSourceChannel) {
        this.paymentSourceChannel = paymentSourceChannel;
    }

    public TimelineElementDetails uncertainPaymentDate(Boolean uncertainPaymentDate) {
        this.uncertainPaymentDate = uncertainPaymentDate;
        return this;
    }
    public Boolean getUncertainPaymentDate() {
        return uncertainPaymentDate;
    }

    public void setUncertainPaymentDate(Boolean uncertainPaymentDate) {
        this.uncertainPaymentDate = uncertainPaymentDate;
    }

    public TimelineElementDetails schedulingAnalogDate(OffsetDateTime schedulingAnalogDate) {
        this.schedulingAnalogDate = schedulingAnalogDate;
        return this;
    }

    public OffsetDateTime getSchedulingAnalogDate() {
        return schedulingAnalogDate;
    }

    public void setSchedulingAnalogDate(OffsetDateTime schedulingAnalogDate) {
        this.schedulingAnalogDate = schedulingAnalogDate;
    }
}

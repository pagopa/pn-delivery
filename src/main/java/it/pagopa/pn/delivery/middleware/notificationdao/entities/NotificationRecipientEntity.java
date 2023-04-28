package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationRecipientEntity {
    private RecipientTypeEntity recipientType;
    private String recipientId;
    private String denomination;
    private NotificationDigitalAddressEntity digitalDomicile;
    private NotificationPhysicalAddressEntity physicalAddress;
    private List<NotificationPaymentInfoEntity> payments;

    @DynamoDbAttribute(value = "recipientType")
    public RecipientTypeEntity getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientTypeEntity recipientType) {
        this.recipientType = recipientType;
    }

    @DynamoDbAttribute(value = "recipientId")
    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    @DynamoDbAttribute(value = "denomination")
    public String getDenomination() {
        return denomination;
    }

    public void setDenomination(String denomination) {
        this.denomination = denomination;
    }

    @DynamoDbAttribute(value = "digitalDomicile")
    public NotificationDigitalAddressEntity getDigitalDomicile() {
        return digitalDomicile;
    }

    public void setDigitalDomicile(NotificationDigitalAddressEntity digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
    }

    @DynamoDbAttribute(value = "physicalAddress")
    public NotificationPhysicalAddressEntity getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(NotificationPhysicalAddressEntity physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    @DynamoDbAttribute(value = "payments")
    public List<NotificationPaymentInfoEntity> getPayments() {
        return payments;
    }

    public void setPayments(List<NotificationPaymentInfoEntity> payments) {
        this.payments = payments;
    }
}

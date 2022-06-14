package it.pagopa.pn.delivery.middleware.notificationdao.entities;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class NotificationRecipientEntity {
    private RecipientTypeEntity recipientType;
    private String taxId;
    private String denomination;
    private NotificationDigitalAddressEntity digitalDomicile;
    private NotificationPhysicalAddressEntity physicalAddress;
    private NotificationPaymentInfoEntity payment;

    @DynamoDbAttribute(value = "recipientType")
    public RecipientTypeEntity getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientTypeEntity recipientType) {
        this.recipientType = recipientType;
    }

    @DynamoDbAttribute(value = "taxId")
    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
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

    @DynamoDbAttribute(value = "payment")
    public NotificationPaymentInfoEntity getPayment() {
        return payment;
    }

    public void setPayment(NotificationPaymentInfoEntity payment) {
        this.payment = payment;
    }
}

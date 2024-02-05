package it.pagopa.pn.delivery.models.internal.notification;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV23;
import lombok.*;

import javax.validation.Valid;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecipient {
    private NotificationRecipientV23.RecipientTypeEnum recipientType;
    private String taxId;
    private String internalId;
    private String denomination;
    private NotificationDigitalAddress digitalDomicile;
    private NotificationPhysicalAddress physicalAddress;
    private List<NotificationPaymentInfo> payments = null;

    public NotificationRecipient recipientType(NotificationRecipientV23.RecipientTypeEnum recipientType) {
        this.recipientType = recipientType;
        return this;
    }

    public NotificationRecipientV23.RecipientTypeEnum getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(NotificationRecipientV23.RecipientTypeEnum recipientType) {
        this.recipientType = recipientType;
    }

    public NotificationRecipient taxId(String taxId) {
        this.taxId = taxId;
        return this;
    }

  public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public NotificationRecipient internalId(String internalId) {
        this.internalId = internalId;
        return this;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public NotificationRecipient denomination(String denomination) {
        this.denomination = denomination;
        return this;
    }

    public String getDenomination() {
        return denomination;
    }

    public void setDenomination(String denomination) {
        this.denomination = denomination;
    }

    public NotificationRecipient digitalDomicile(NotificationDigitalAddress digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
        return this;
    }

    public NotificationDigitalAddress getDigitalDomicile() {
        return digitalDomicile;
    }

    public void setDigitalDomicile(NotificationDigitalAddress digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
    }

    public NotificationRecipient physicalAddress(NotificationPhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
        return this;
    }

    public NotificationPhysicalAddress getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(NotificationPhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    public NotificationRecipient payment(List<NotificationPaymentInfo> payments) {
        this.payments = payments;
        return this;
    }

    /**
     * Get payment
     * @return payment
     */
    @Valid
    public List<NotificationPaymentInfo> getPayments() {
        return payments;
    }

    public void setPayment(List<NotificationPaymentInfo> payments) {
        this.payments = payments;
    }

}

package it.pagopa.pn.delivery.model.notification;


import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;

public class NotificationRecipient {

    private String fc;
    private DigitalAddress digitalDomicile;
    private PhysicalAddress physicalAddress;

    public String getFc() {
        return fc;
    }

    public void setFc(String fc) {
        this.fc = fc;
    }

    public DigitalAddress getDigitalDomicile() {
        return digitalDomicile;
    }

    public void setDigitalDomicile(DigitalAddress digitalDomicile) {
        this.digitalDomicile = digitalDomicile;
    }

    public PhysicalAddress getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(PhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

}

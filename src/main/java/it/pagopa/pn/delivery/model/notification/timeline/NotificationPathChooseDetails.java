package it.pagopa.pn.delivery.model.notification.timeline;

public class NotificationPathChooseDetails {

    public enum DeliveryMode {
        DIGITAL,
        ANALOG
    }

    private String fc;
    private DeliveryMode deliveryMode;

    public String getFc() {
        return fc;
    }

    public void setFc(String fc) {
        this.fc = fc;
    }

    public DeliveryMode getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(DeliveryMode deliveryMode) {
        this.deliveryMode = deliveryMode;
    }
}

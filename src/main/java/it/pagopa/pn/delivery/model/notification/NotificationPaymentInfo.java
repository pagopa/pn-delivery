package it.pagopa.pn.delivery.model.notification;

import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@UserDefinedType
public class NotificationPaymentInfo {

    public enum FeePolicies {
        FLAT_RATE,
        DELIVERY_MODE,
    }

    private String iuv;
    private FeePolicies notificationFeePolicy;
    private F24 f24 = new F24();

    public String getIuv() {
        return iuv;
    }

    public void setIuv(String iuv) {
        this.iuv = iuv;
    }

    public FeePolicies getNotificationFeePolicy() {
        return notificationFeePolicy;
    }

    public void setNotificationFeePolicy(FeePolicies notificationFeePolicy) {
        this.notificationFeePolicy = notificationFeePolicy;
    }

    public F24 getF24() {
        return f24;
    }

    public void setF24(F24 f24) {
        this.f24 = f24;
    }

    @UserDefinedType
    public static class F24 {
        private NotificationAttachment flatRate;
        private NotificationAttachment digital;
        private NotificationAttachment analog;

        public NotificationAttachment getFlatRate() {
            return flatRate;
        }

        public void setFlatRate(NotificationAttachment flatRate) {
            this.flatRate = flatRate;
        }

        public NotificationAttachment getDigital() {
            return digital;
        }

        public void setDigital(NotificationAttachment digital) {
            this.digital = digital;
        }

        public NotificationAttachment getAnalog() {
            return analog;
        }

        public void setAnalog(NotificationAttachment analog) {
            this.analog = analog;
        }
    }
}

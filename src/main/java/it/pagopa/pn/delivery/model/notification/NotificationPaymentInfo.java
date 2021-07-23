package it.pagopa.pn.delivery.model.notification;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.UserDefinedType;

@Data
@Builder
@UserDefinedType
public class NotificationPaymentInfo {

    public enum FeePolicies {
        FLAT_RATE,
        DELIVERY_MODE,
    }

    private String iuv;
    private FeePolicies notificationFeePolicy;
    private F24 f24 ;

    @Data
    @Builder
    @UserDefinedType
    public static class F24 {
        private NotificationAttachment flatRate;
        private NotificationAttachment digital;
        private NotificationAttachment analog;

    }
}

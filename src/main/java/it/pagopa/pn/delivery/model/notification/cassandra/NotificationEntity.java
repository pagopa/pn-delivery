package it.pagopa.pn.delivery.model.notification.cassandra;


import java.util.List;
import java.util.Map;

import it.pagopa.pn.api.dto.notification.NotificationPaymentInfoFeePolicies;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("Notification")
@Data
@Builder
public class NotificationEntity {

    @PrimaryKey
    private String iun;

    private String paNotificationId;

    private String subject;

    private String cancelledIun;

    private String cancelledByIun;

    private String senderPaId;

    private Map<String,String> recipientsJson;

    private List<String> documentsDigestsSha256 ;

    private String iuv;

    private NotificationPaymentInfoFeePolicies notificationFeePolicy;

    private String f24FlatRateDigestSha256;

    private String f24DigitalDigestSha256;

    private String f24AnalogDigestSha256;

}

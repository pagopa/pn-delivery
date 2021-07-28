package it.pagopa.pn.delivery.model.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationAck {

    private String iun;
    private String paNotificationId;

}

package it.pagopa.pn.delivery.models;

import lombok.Data;

@Data
public class NotificationRefusedPayload {
    private String iun;
    private String paId;
    private String sentAt;
    private String timeLineId;
}

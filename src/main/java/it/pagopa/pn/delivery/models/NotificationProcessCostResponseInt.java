package it.pagopa.pn.delivery.models;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class NotificationProcessCostResponseInt {
    private Integer partialCost;
    private Integer totalCost;
    private Integer analogCost;
    private Integer paFee;
    private Integer sendFee;
    private Integer vat;
    private OffsetDateTime refinementDate;
    private OffsetDateTime notificationViewDate;

}

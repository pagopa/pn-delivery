package it.pagopa.pn.delivery.models;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class InternalNotificationCost {
    private String creditorTaxId_noticeCode;
    private String iun;
    private int recipientIdx;

}

package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPaymentInfo {
    private String noticeCode;
    private String creditorTaxId;
    private Boolean applyCost;
    private String noticeCodeAlternative;
    private NotificationPaymentAttachment pagoPaForm;
    private F24Payment f24;
}

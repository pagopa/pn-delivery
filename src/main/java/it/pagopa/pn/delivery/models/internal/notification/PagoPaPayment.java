package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PagoPaPayment {
    private String noticeCode;
    private String creditorTaxId;
    private boolean applyCost;
    private String noticeCodeAlternative;
    private MetadataAttachment attachment;
}

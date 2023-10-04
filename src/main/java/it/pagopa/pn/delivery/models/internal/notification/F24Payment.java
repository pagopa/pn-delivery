package it.pagopa.pn.delivery.models.internal.notification;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class F24Payment {
    private String title;
    private boolean applyCost;
    private Integer index;
    private MetadataAttachment metadataAttachment;
}

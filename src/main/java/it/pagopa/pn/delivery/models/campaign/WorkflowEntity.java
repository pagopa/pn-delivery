package it.pagopa.pn.delivery.models.campaign;

import lombok.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowEntity {
    private String channel;
    private String desiredFeedback;
    private boolean includeAttachment;
}

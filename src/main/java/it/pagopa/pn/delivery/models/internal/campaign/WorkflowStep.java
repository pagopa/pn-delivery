package it.pagopa.pn.delivery.models.internal.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {
    private String channel;
    private String desiredFeedback;
    private Boolean includeAttachment;
}


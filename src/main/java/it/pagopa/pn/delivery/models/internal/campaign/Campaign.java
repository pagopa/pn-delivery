package it.pagopa.pn.delivery.models.internal.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    private String campaignId;
    private String senderId;
    private String title;
    private String descriptionScope;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private Boolean closed;
    private String senderContact;
    private String serviceId;
    private Boolean sensitiveContent;
    private Boolean stopOnViewed;
    private List<WorkflowStep> workflow;
}


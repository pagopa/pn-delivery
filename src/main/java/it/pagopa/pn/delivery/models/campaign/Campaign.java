package it.pagopa.pn.delivery.models.campaign;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @NotEmpty
    private String campaignId;

    @NotEmpty
    private String senderId;

    @NotEmpty
    private String title;

    @NotEmpty
    private String descriptionScope;

    @NotNull
    private Instant startDate;

    @NotNull
    private Instant endDate;

    private boolean closed;

    private String senderContact;

    @NotEmpty
    private String serviceId;

    @Valid
    private List<Message> messages;

    private boolean sensitiveContent;

    private boolean stopOnViewed;

    @Valid
    @NotEmpty
    private List<WorkflowEntity> workflow;
}

package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InputSearchNotificationDto {
    @NotEmpty
    private String senderReceiverId;

    private String campaignId;
    
    @NotNull
    private Instant startDate;
    
    @NotNull
    private Instant endDate;

    private String mandateId;

    private List<String> mandateAllowedPaIds;
    
    private String filterId;

    private String opaqueFilterIdPF;

    private String opaqueFilterIdPG;
    
    private  List<NotificationStatusV26> statuses;

    // filtro di stato per il flusso bonario (campagna): gli stati bonari non sono rappresentabili con NotificationStatusV26
    private List<InformalNotificationStatusV1> informalStatuses;

    private List<String> groups;
    
    private String subjectRegExp;

    private String iunMatch;

    private NotificationSearchCommunicationType communicationType;

    private Boolean viewed;

    private Boolean delivered;

    private boolean receiverIdIsOpaque;

    @Positive
    @NotNull
    private Integer size;
    
    private String nextPagesKey;
    
    private boolean bySender;

    private boolean byCampaign;

    private Integer maxPageNumber;
    
}

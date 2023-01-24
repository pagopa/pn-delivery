package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatus;
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
    
    @NotNull
    private Instant startDate;
    
    @NotNull
    private Instant endDate;

    private String mandateId;
    
    private String filterId;

    private String opaqueFilterIdCF;

    private String opaqueFilterIdPIva;
    
    private  List<NotificationStatus> statuses;

    private List<String> groups;
    
    private String subjectRegExp;

    private String iunMatch;

    private boolean receiverIdIsOpaque;

    @Positive
    @NotNull
    private Integer size;
    
    private String nextPagesKey;
    
    private boolean bySender;

    private Integer maxPageNumber;
    
}

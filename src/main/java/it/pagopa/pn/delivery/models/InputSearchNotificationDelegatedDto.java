package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputSearchNotificationDelegatedDto {

    @NotEmpty
    private String delegateId;

    @NotNull
    private Instant startDate;

    @NotNull
    private Instant endDate;

    private String group;

    private String senderId;

    private String receiverId;

    private List<NotificationStatusV26> statuses;

    private String iun;

    @Positive
    @NotNull
    private Integer size;

    private String nextPageKey;

    private Integer maxPageNumber;

    private List<String> cxGroups;

}

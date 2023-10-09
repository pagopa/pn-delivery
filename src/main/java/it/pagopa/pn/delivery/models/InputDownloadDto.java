package it.pagopa.pn.delivery.models;

import lombok.*;

import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InputDownloadDto {
    private String iun;
    private String cxType;
    private String cxId;
    private String uid;
    private String mandateId;

    private List<String> cxGroups;
    private Integer documentIndex;
    private Integer recipientIdx;
    private String attachmentName;
    private Integer attachmentIdx;
    private Boolean markNotificationAsViewed;
}

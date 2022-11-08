package it.pagopa.pn.delivery.models;

import lombok.*;

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
    private String mandateId;
    private Integer documentIndex;
    private Integer recipientIdx;
    private String attachmentName;
    private Boolean markNotificationAsViewed;
}

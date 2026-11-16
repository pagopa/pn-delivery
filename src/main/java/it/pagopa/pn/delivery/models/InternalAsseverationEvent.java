package it.pagopa.pn.delivery.models;

import lombok.*;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class InternalAsseverationEvent {

    @NotEmpty
    private String iun;

    @NotEmpty
    private String senderPaId;

    @NotEmpty
    private String notificationSentAt;

    @NotEmpty
    private String noticeCode;

    @NotEmpty
    private String creditorTaxId;

    @NotEmpty
    private String debtorPosUpdateDate;

    @NotEmpty
    private String recordCreationDate;

    private int recipientIdx;

    private int version;

    @Nullable
    private AsseverationEvent.Payload.AsseverationMoreField moreFields;

}

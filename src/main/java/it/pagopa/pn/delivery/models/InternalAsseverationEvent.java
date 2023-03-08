package it.pagopa.pn.delivery.models;

import lombok.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;

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

    @NotNull
    private Instant notificationSentAt;

    @NotEmpty
    private String noticeCode;

    @NotEmpty
    private String creditorTaxId;

    @NotNull
    private Instant debtorPosUpdateDate;

    @NotNull
    private Instant recordCreationDate;

    @NotEmpty
    private String recipientId;

    @NotNull
    private AsseverationEvent.RecipientType recipientType;

    private int recipientIdx;

    private int version;

    @Nullable
    private Object payload;
}

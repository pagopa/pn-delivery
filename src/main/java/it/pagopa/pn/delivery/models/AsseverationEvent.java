package it.pagopa.pn.delivery.models;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class AsseverationEvent implements GenericEvent<StandardEventHeader, AsseverationEvent.Payload> {
    private StandardEventHeader header;
    private Payload payload;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class Payload {

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
        private RecipientType recipientType;

        private int recipientIdx;

        private int version;

        @Nullable
        private Object asseverationPayload;
    }

    public enum RecipientType {
        PF("PF"),

        PG("PG");

        private final String value;

        RecipientType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}

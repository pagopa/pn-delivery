package it.pagopa.pn.delivery.models.campaign;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    public enum AdditionalLanguage {
        DE,
        SL,
        FR
    }

    public enum PrimaryLanguage {
        IT
    }

    private AdditionalLanguage additionalLanguage;

    @Builder.Default
    @NotNull
    private PrimaryLanguage primaryLanguage = PrimaryLanguage.IT;

    @NotEmpty
    private String messageId;
}

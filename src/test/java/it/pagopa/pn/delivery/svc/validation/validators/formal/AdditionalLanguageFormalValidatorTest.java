package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalLanguageFormalValidatorTest {

    private final AdditionalLanguageFormalValidator validator = new AdditionalLanguageFormalValidator();

    @Test
    void shouldReturnSuccessWhenNoAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(null), null));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenMoreThanOneAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("DE", "FR")), campaign(it.pagopa.pn.delivery.models.campaign.Message.AdditionalLanguage.DE)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_MAX_SIZE_EXCEEDED.getValue());
    }

    @Test
    void shouldReturnErrorWhenAdditionalLanguageIsNotSupported() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("EN")), campaign(it.pagopa.pn.delivery.models.campaign.Message.AdditionalLanguage.DE)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("DE,FR,SL");
    }

    private InternalNotification buildNotificationWithAdditionalLanguages(List<String> additionalLanguages) {
        return InternalNotification.builder()
                .additionalLanguages(additionalLanguages)
                .build();
    }
}


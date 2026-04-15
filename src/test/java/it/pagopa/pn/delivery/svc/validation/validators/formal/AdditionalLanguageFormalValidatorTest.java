package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.campaign;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.informalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalLanguageFormalValidatorTest {

    private final AdditionalLanguageFormalValidator validator = new AdditionalLanguageFormalValidator();

    @Test
    void shouldReturnSuccessWhenNoAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), null, campaign(it.pagopa.pn.delivery.models.campaign.Message.AdditionalLanguage.DE)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenMoreThanOneAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), List.of("DE", "FR"), campaign(it.pagopa.pn.delivery.models.campaign.Message.AdditionalLanguage.DE)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_REQUIRED_ADDITIONAL_LANG.getValue());
    }

    @Test
    void shouldReturnErrorWhenAdditionalLanguageIsNotSupported() {
        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), List.of("EN"), campaign(it.pagopa.pn.delivery.models.campaign.Message.AdditionalLanguage.DE)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_INVALID_ADDITIONAL_LANG.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("DE,FR,SL");
    }
}


package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalLanguageFormalValidatorTest {

    private final AdditionalLanguageFormalValidator validator = new AdditionalLanguageFormalValidator();

    @Test
    void shouldReturnSuccessWhenNoRecipientsArePresent() {
        InternalNotification notification = InternalNotification.builder()
                .recipients(null)
                .build();

        ValidationResult result = validator.validate(informalContext(notification));

        assertSuccess(result);
    }

    @Test
    void shouldReturnSuccessWhenNoAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithRecipientAdditionalLanguages(null)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenMoreThanOneAdditionalLanguageIsRequested() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithRecipientAdditionalLanguages(List.of("DE", "FR"))));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_MAX_SIZE_EXCEEDED.getValue());
        assertThat(result.getErrors().get(0).getElement()).isEqualTo("recipients[0].additionalLanguages");
    }

    @Test
    void shouldReturnErrorWhenAdditionalLanguageIsNotSupported() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithRecipientAdditionalLanguages(List.of("EN"))));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("DE,FR,SL");
        assertThat(result.getErrors().get(0).getElement()).isEqualTo("recipients[0].additionalLanguages");
    }

    @Test
    void shouldReturnMultipleErrorsWhenMultipleRecipientsHaveInvalidLanguages() {
        NotificationRecipient recipient0 = NotificationRecipient.builder()
                .additionalLanguages(List.of("DE", "FR"))
                .build();
        NotificationRecipient recipient1 = NotificationRecipient.builder()
                .additionalLanguages(List.of("EN"))
                .build();

        InternalNotification notification = InternalNotification.builder()
                .recipients(List.of(recipient0, recipient1))
                .build();

        ValidationResult result = validator.validate(informalContext(notification));

        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_MAX_SIZE_EXCEEDED.getValue());
        assertThat(result.getErrors().get(0).getElement()).isEqualTo("recipients[0].additionalLanguages");
        assertThat(result.getErrors().get(1).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(1).getElement()).isEqualTo("recipients[1].additionalLanguages");
    }

    @Test
    void shouldReturnSuccessWhenOneRecipientHasValidAdditionalLanguage() {
        ValidationResult result = validator.validate(informalContext(buildNotificationWithRecipientAdditionalLanguages(List.of("DE"))));

        assertSuccess(result);
    }

    private InternalNotification buildNotificationWithRecipientAdditionalLanguages(List<String> additionalLanguages) {
        NotificationRecipient recipient = NotificationRecipient.builder()
                .additionalLanguages(additionalLanguages)
                .build();

        return InternalNotification.builder()
                .recipients(List.of(recipient))
                .build();
    }
}


package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;
import static org.assertj.core.api.Assertions.assertThat;

class DenominationAndAtValidatorTest {

    @Test
    void shouldReturnErrorWhenDenominationExceedsConfiguredLength() {
        DenominationAndAtValidator validator = new DenominationAndAtValidator(4, "NONE", null, null);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "TOO_LONG", physicalAddress(), List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_DENOMINATION_LENGTH_EXCEEDED.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("exceed max length");
    }

    @Test
    void shouldReturnErrorWhenAtContainsInvalidCharactersForConfiguredRegex() {
        DenominationAndAtValidator validator = new DenominationAndAtValidator(0, "REGEX", "a-zA-Z", null);
        NotificationPhysicalAddress address = physicalAddress();
        address.setAt("A1");
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Valid", address, List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_DENOMINATION_INVALID_CHARACTERS.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("contains invalid characters");
    }

    @Test
    void shouldReturnSuccessWhenConfigurationDoesNotApplyAnyConstraint() {
        DenominationAndAtValidator validator = new DenominationAndAtValidator(0, "NONE", null, null);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Valid Denomination", physicalAddress(), List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

        assertSuccess(result);
    }
}


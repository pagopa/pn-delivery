package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class PhysicalAddressValidatorTest {

    @Test
    void shouldReturnSuccessWhenValidationIsDisabledAndAddressIsPresent() {
        PhysicalAddressValidator validator = new PhysicalAddressValidator(false, 10, "a-zA-Z");
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorWhenPhysicalAddressIsNull() {
        PhysicalAddressValidator validator = new PhysicalAddressValidator(true, 10, "a-zA-Z0-9 ");
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", null, List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_NULL.getValue(),
                "PhysicalAddress cannot be null"
        );
    }

    @Test
    void shouldReturnErrorWhenFieldContainsInvalidCharacters() {
        PhysicalAddressValidator validator = new PhysicalAddressValidator(true, 100, "a-zA-Z0-9 ");
        NotificationPhysicalAddress address = physicalAddress();
        address.setAddress("via@roma");
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", address, List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_INVALID_CHARACTERS.getValue(),
                "contains invalid characters"
        );
    }

    @Test
    void shouldReturnErrorWhenRowLengthExceedsConfiguredLimit() {
        PhysicalAddressValidator validator = new PhysicalAddressValidator(true, 5, "a-zA-Z0-9 ");
        NotificationPhysicalAddress address = physicalAddress("abc", "abc", "abc", "12345", "abc", "abc", "AA", "FR");
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", address, List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

        org.assertj.core.api.Assertions.assertThat(result.isFailure()).isTrue();
        org.assertj.core.api.Assertions.assertThat(result.getErrors())
                .allSatisfy(error -> {
                    org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo(ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_LENGTH_EXCEEDED.getValue());
                    org.assertj.core.api.Assertions.assertThat(error.getDetail()).contains("exceed max length");
                });
    }
}

package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.util.PhysicalAddressLookupUtil;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PhysicalAddressValidatorTest {

    @Mock
    private PhysicalAddressLookupUtil physicalAddressLookupUtil;

    @Mock
    private PnDeliveryConfigs cfg;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private PhysicalAddressValidator validator(boolean validationActivated, int length, String pattern) {
        when(cfg.isPhysicalAddressValidation()).thenReturn(validationActivated);
        when(cfg.getPhysicalAddressValidationLength()).thenReturn(length);
        when(cfg.getPhysicalAddressValidationPattern()).thenReturn(pattern);
        return new PhysicalAddressValidator(physicalAddressLookupUtil, cfg);
    }

    @Test
    void shouldReturnSuccessWhenValidationIsDisabledAndAddressIsPresent() {
        PhysicalAddressValidator validator = validator(false, 10, "a-zA-Z");
        when(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled(anyString())).thenReturn(false);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorWhenPhysicalAddressIsNull() {
        PhysicalAddressValidator validator = validator(true, 10, "a-zA-Z0-9 ");
        when(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled(anyString())).thenReturn(false);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", null, List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_NULL.getValue(),
                "PhysicalAddress cannot be null"
        );
    }

    @Test
    void shouldSkipNullAddressValidationWhenPhysicalAddressLookupIsEnabled() {
        PhysicalAddressValidator validator = validator(true, 10, "a-zA-Z0-9 ");
        when(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled(anyString())).thenReturn(true);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", null, List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorWhenFieldContainsInvalidCharacters() {
        PhysicalAddressValidator validator = validator(true, 100, "a-zA-Z0-9 ");
        when(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled(anyString())).thenReturn(false);
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
        PhysicalAddressValidator validator = validator(true, 5, "a-zA-Z0-9 ");
        when(physicalAddressLookupUtil.checkPhysicalAddressLookupIsEnabled(anyString())).thenReturn(false);
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

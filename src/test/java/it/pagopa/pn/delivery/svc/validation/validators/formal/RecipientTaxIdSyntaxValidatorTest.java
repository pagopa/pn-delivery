package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipientTaxIdSyntaxValidatorTest {

    @Test
    void shouldReturnSuccessForValidPfTaxIdWithoutExternalValidation() {
        ValidateUtils validateUtils = mock(ValidateUtils.class);
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        when(validateUtils.validate("AAAAAA00A00A000A", true, false, false)).thenReturn(true);
        when(cfg.isSkipCheckTaxIdInBlackList()).thenReturn(false);
        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(false);

        RecipientTaxIdSyntaxValidator validator = new RecipientTaxIdSyntaxValidator(validateUtils, cfg);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorForInvalidPfTaxId() {
        ValidateUtils validateUtils = mock(ValidateUtils.class);
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        when(validateUtils.validate("INVALID", true, false, false)).thenReturn(false);
        when(cfg.isSkipCheckTaxIdInBlackList()).thenReturn(false);

        RecipientTaxIdSyntaxValidator validator = new RecipientTaxIdSyntaxValidator(validateUtils, cfg);
        NotificationRecipient recipient = pfRecipient("INVALID", "Mario Rossi", physicalAddress(), List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_INVALID_TAX_ID.getValue(),
                "Invalid taxId for recipient 0"
        );
    }

    @Test
    void shouldReturnErrorForPgRecipientWithNonNumericTaxId() {
        ValidateUtils validateUtils = mock(ValidateUtils.class);
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        when(validateUtils.validate("ABC123", false, false, false)).thenReturn(true);
        when(cfg.isSkipCheckTaxIdInBlackList()).thenReturn(false);
        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(false);

        RecipientTaxIdSyntaxValidator validator = new RecipientTaxIdSyntaxValidator(validateUtils, cfg);
        NotificationRecipient recipient = pgRecipient("ABC123", "Azienda Spa", physicalAddress(), List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors())
                .anySatisfy(error -> {
                    assertThat(error.getCode()).isEqualTo(ErrorCodes.ERROR_CODE_PG_TAX_ID_NOT_NUMERICAL.getValue());
                    assertThat(error.getDetail()).contains("only numerical taxId");
                });
    }

    @Test
    void shouldReturnErrorForDuplicatedTaxIds() {
        ValidateUtils validateUtils = mock(ValidateUtils.class);
        PnDeliveryConfigs cfg = mock(PnDeliveryConfigs.class);
        when(validateUtils.validate("AAAAAA00A00A000A", true, false, false)).thenReturn(true);
        when(cfg.isSkipCheckTaxIdInBlackList()).thenReturn(false);
        when(cfg.isEnableTaxIdExternalValidation()).thenReturn(false);

        RecipientTaxIdSyntaxValidator validator = new RecipientTaxIdSyntaxValidator(validateUtils, cfg);
        NotificationRecipient first = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());
        NotificationRecipient second = pfRecipient("AAAAAA00A00A000A", "Ada Lovelace", physicalAddress(), List.of());

        ValidationResult result = validator.validate(legalContext(notification(List.of(first, second), List.of())));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors())
                .anySatisfy(error -> {
                    assertThat(error.getCode()).isEqualTo(ErrorCodes.ERROR_CODE_DUPLICATED_RECIPIENT_TAX_ID.getValue());
                    assertThat(error.getDetail()).contains("Duplicated recipient taxId");
                });
    }
}

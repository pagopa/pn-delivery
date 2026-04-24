package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pgRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class PgTaxIdValidatorTest {

    private final PgTaxIdValidator validator = new PgTaxIdValidator();

    @Test
    void shouldReturnSuccessForPfRecipient() {
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnSuccessForPgRecipientWithNumericTaxId() {
        NotificationRecipient recipient = pgRecipient("12345678901", "Azienda Spa", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorForPgRecipientWithNonNumericTaxId() {
        NotificationRecipient recipient = pgRecipient("ABC123XYZ", "Azienda Spa", physicalAddress(), List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PG_TAX_ID_NOT_NUMERICAL.getValue(),
                "only numerical taxId"
        );
    }
}

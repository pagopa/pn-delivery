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
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class MaxRecipientsSizeValidatorTest {

    @Test
    void shouldReturnSuccessWhenRecipientsAreWithinLimit() {
        MaxRecipientsSizeValidator validator = new MaxRecipientsSizeValidator(2);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorWhenRecipientsExceedLimit() {
        MaxRecipientsSizeValidator validator = new MaxRecipientsSizeValidator(1);
        NotificationRecipient first = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());
        NotificationRecipient second = pfRecipient("BBBBBB00B00B000B", "Ada Lovelace", physicalAddress(), List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(first, second), List.of()))),
                ErrorCodes.ERROR_CODE_MAX_RECIPIENT.getValue(),
                "Max recipient count reached"
        );
    }
}

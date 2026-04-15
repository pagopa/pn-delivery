package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.f24Payment;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class MaxPaymentsSizeValidatorTest {

    @Test
    void shouldReturnSuccessWhenPaymentsAreWithinLimit() {
        MaxPaymentsSizeValidator validator = new MaxPaymentsSizeValidator(2);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of(f24Payment("PN_F24_META-ok.json", "sha-1", "application/json")));

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorWhenPaymentsExceedLimit() {
        MaxPaymentsSizeValidator validator = new MaxPaymentsSizeValidator(1);
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(
                        f24Payment("PN_F24_META-1.json", "sha-1", "application/json"),
                        f24Payment("PN_F24_META-2.json", "sha-2", "application/json")
                )
        );

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_MAX_PAYMENT.getValue(),
                "Max payment count reached"
        );
    }
}
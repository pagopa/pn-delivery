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
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pagoPaPayment;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class PaymentAttachmentValidatorTest {

    private final PaymentAttachmentValidator validator = new PaymentAttachmentValidator();

    @Test
    void shouldReturnSuccessForValidPagoPaAndF24Attachments() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(
                        pagoPaPayment("PN_NOTIFICATION_ATTACHMENTS-doc.pdf", "sha-1", "application/pdf"),
                        f24Payment("PN_F24_META-doc.json", "sha-2", "application/json")
                )
        );

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }

    @Test
    void shouldReturnErrorForInvalidPagoPaAttachmentKey() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(pagoPaPayment("invalid-key.json", "sha-1", "application/pdf"))
        );

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PAYMENT_ATTACHMENT_CONTENT_TYPE.getValue(),
                "does not conform to the expected content type"
        );
    }

    @Test
    void shouldReturnErrorForInvalidF24AttachmentKey() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(f24Payment("PN_F24_META-invalid.pdf", "sha-1", "application/json"))
        );

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of()))),
                ErrorCodes.ERROR_CODE_PAYMENT_ATTACHMENT_CONTENT_TYPE.getValue(),
                "does not conform to the expected content type"
        );
    }

    @Test
    void shouldReturnSuccessWhenPaymentsAreNull() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                null
        );

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
    }
}

package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleError;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.document;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pagoPaPayment;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class UniqueAttachmentsValidatorTest {

    private final UniqueAttachmentsValidator validator = new UniqueAttachmentsValidator();

    @Test
    void shouldReturnSuccessWhenAllAttachmentsAreDistinct() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(pagoPaPayment("PN_NOTIFICATION_ATTACHMENTS-1.pdf", "sha-2", "application/pdf"))
        );
        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"))))));
    }

    @Test
    void shouldReturnErrorWhenDocumentsContainDuplicateAttachments() {
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());
        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"), document("key-1", "sha-1"))))),
                ErrorCodes.ERROR_CODE_DUPLICATED_ATTACHMENTS.getValue(),
                "Same attachment compares more then once in the same request"
        );
    }

    @Test
    void shouldReturnErrorWhenRecipientPaymentDuplicatesDocumentAttachment() {
        NotificationRecipient recipient = pfRecipient(
                "AAAAAA00A00A000A",
                "Mario Rossi",
                physicalAddress(),
                List.of(pagoPaPayment("key-1", "sha-1", "application/pdf"))
        );
        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"))))),
                ErrorCodes.ERROR_CODE_DUPLICATED_ATTACHMENTS.getValue(),
                "Same attachment compares more then once in the same request"
        );
    }
}

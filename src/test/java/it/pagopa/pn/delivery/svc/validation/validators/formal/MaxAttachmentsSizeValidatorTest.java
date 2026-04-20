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
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;

class MaxAttachmentsSizeValidatorTest {

    @Test
    void shouldReturnSuccessWhenAttachmentsAreWithinLimit() {
        MaxAttachmentsSizeValidator validator = new MaxAttachmentsSizeValidator(2);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"))))));
    }

    @Test
    void shouldReturnErrorWhenAttachmentsExceedLimit() {
        MaxAttachmentsSizeValidator validator = new MaxAttachmentsSizeValidator(1);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"), document("key-2", "sha-2"))))),
                ErrorCodes.ERROR_CODE_MAX_ATTACHMENT.getValue(),
                "Max attachment count reached"
        );
    }

    @Test
    void shouldSkipValidationWhenAttachmentsLimitIsMinusOne() {
        MaxAttachmentsSizeValidator validator = new MaxAttachmentsSizeValidator(-1);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSuccess(
                validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"), document("key-2", "sha-2")))))
        );
    }

    @Test
    void shouldTreatZeroAsAnActiveAttachmentsLimit() {
        MaxAttachmentsSizeValidator validator = new MaxAttachmentsSizeValidator(0);
        NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

        assertSingleError(
                validator.validate(legalContext(notification(List.of(recipient), List.of(document("key-1", "sha-1"))))),
                ErrorCodes.ERROR_CODE_MAX_ATTACHMENT.getValue(),
                "Max attachment count reached"
        );
    }
}


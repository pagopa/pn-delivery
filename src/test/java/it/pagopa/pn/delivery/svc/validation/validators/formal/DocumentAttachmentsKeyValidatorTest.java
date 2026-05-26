package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentAttachmentsKeyValidatorTest {

    private DocumentAttachmentsKeyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentAttachmentsKeyValidator();
    }

    @Test
    void shouldAcceptDocumentWithValidKey() {
        // Given
        NotificationDocument document = document("some/path/PN_NOTIFICATION_ATTACHMENTS/file.pdf", "sha256");
        InternalNotification payload = notification(List.of(), List.of(document));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertSuccess(result);
    }

    @Test
    void shouldAcceptMultipleDocumentsWithValidKeys() {
        // Given
        NotificationDocument doc1 = document("path/PN_NOTIFICATION_ATTACHMENTS/doc1.pdf", "sha1");
        NotificationDocument doc2 = document("PN_NOTIFICATION_ATTACHMENTS/doc2.pdf", "sha2");
        NotificationDocument doc3 = document("prefix/PN_NOTIFICATION_ATTACHMENTS/suffix/doc3.pdf", "sha3");

        InternalNotification payload = notification(List.of(), List.of(doc1, doc2, doc3));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertSuccess(result);
    }

    @Test
    void shouldRejectDocumentWithNullKey() {
        // Given
        NotificationDocument document = document(null, "sha256");
        InternalNotification payload = notification(List.of(), List.of(document));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getElement()).isEqualTo("documents[0].ref.key");
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_INVALID_DOCUMENT_KEY.getValue());
        assertThat(result.getErrors().get(0).getDetail()).isEqualTo("Document key must contain: PN_NOTIFICATION_ATTACHMENTS");
    }

    @Test
    void shouldRejectDocumentWithKeyMissingRequiredString() {
        // Given
        NotificationDocument document = document("some/path/without/required/string.pdf", "sha256");
        InternalNotification payload = notification(List.of(), List.of(document));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertSingleError(result,
                ErrorCodes.ERROR_CODE_INVALID_DOCUMENT_KEY.getValue(),
                "Document key must contain: PN_NOTIFICATION_ATTACHMENTS");
    }

    @Test
    void shouldRejectDocumentWithEmptyKey() {
        // Given
        NotificationDocument document = document("", "sha256");
        InternalNotification payload = notification(List.of(), List.of(document));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertSingleError(result,
                ErrorCodes.ERROR_CODE_INVALID_DOCUMENT_KEY.getValue(),
                "Document key must contain: PN_NOTIFICATION_ATTACHMENTS");
    }

    @Test
    void shouldRejectMultipleDocumentsWithInvalidKeys() {
        // Given
        NotificationDocument validDoc = document("PN_NOTIFICATION_ATTACHMENTS/valid.pdf", "sha1");
        NotificationDocument invalidDoc1 = document("invalid/path/doc1.pdf", "sha2");
        NotificationDocument invalidDoc2 = document(null, "sha3");

        InternalNotification payload = notification(List.of(), List.of(validDoc, invalidDoc1, invalidDoc2));
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrors()).hasSize(2);

        assertThat(result.getErrors().get(0).getElement()).isEqualTo("documents[1].ref.key");
        assertThat(result.getErrors().get(1).getElement()).isEqualTo("documents[2].ref.key");

        result.getErrors().forEach(error -> {
            assertThat(error.getCode()).isEqualTo(ErrorCodes.ERROR_CODE_INVALID_DOCUMENT_KEY.getValue());
            assertThat(error.getDetail()).isEqualTo("Document key must contain: PN_NOTIFICATION_ATTACHMENTS");
        });
    }

    @Test
    void shouldAcceptEmptyDocumentList() {
        // Given
        InternalNotification payload = notification(List.of(), List.of());
        InformalNotificationContext context = informalContext(payload);

        // When
        ValidationResult result = validator.validate(context);

        // Then
        assertSuccess(result);
    }
}
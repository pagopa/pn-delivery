package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;

import java.util.ArrayList;
import java.util.List;

public class DocumentAttachmentsKeyValidator implements FormalValidator<NotificationContext> {
    private static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        checkDocumentKeys(context,errors);
        return new ValidationResult(errors);
    }

    private void checkDocumentKeys(NotificationContext context, ArrayList<ProblemError> errors) {
        List<NotificationDocument> documents = context.getPayload().getDocuments();

        for (int i = 0; i < documents.size(); i++) {
            NotificationDocument doc = documents.get(i);
            String key = doc.getRef().getKey();

            if (key == null || !key.contains(PN_NOTIFICATION_ATTACHMENTS)) {
                errors.add(ProblemError.builder()
                        .element("documents[" + i + "].ref.key")
                        .code(ErrorCodes.ERROR_CODE_INVALID_DOCUMENT_KEY.getValue())
                        .detail("Document key must contain: " + PN_NOTIFICATION_ATTACHMENTS)
                        .build());
            }
        }
    }
}

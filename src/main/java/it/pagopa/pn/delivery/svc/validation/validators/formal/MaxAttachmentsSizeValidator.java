package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class MaxAttachmentsSizeValidator implements FormalValidator<NotificationContext> {

    private final Integer maxAttachments;

    @Override
    public ValidationResult validate(NotificationContext context) {

        ArrayList<ProblemError> errors = new ArrayList<>();
        checkMaxAttachments(context, errors);
        return new ValidationResult(errors);
    }

    private void checkMaxAttachments(NotificationContext context, ArrayList<ProblemError> errors) {
        if (maxAttachments > 0 && context.getPayload().getDocuments().size() > maxAttachments) {
            errors.add(ProblemError.builder().element("documents").code(ErrorCodes.ERROR_CODE_MAX_ATTACHMENT.getValue()).detail("Max attachment count reached").build());
        }
    }
}

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
public class MaxRecipientsSizeValidator implements FormalValidator<NotificationContext> {

    private final Integer maxRecipients;

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        checkMaxRecipients(context, errors);
        return new ValidationResult(errors);
    }

    private void checkMaxRecipients(NotificationContext context, ArrayList<ProblemError> errors) {
        if (maxRecipients > 0 && context.getPayload().getRecipients().size() > maxRecipients) {
            errors.add(ProblemError.builder().element("recipients").code(ErrorCodes.ERROR_CODE_MAX_RECIPIENT.getValue()).detail("Max recipient count reached").build());
        }
    }
}

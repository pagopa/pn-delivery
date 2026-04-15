package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.context.NotificaContext;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class MaxPaymentsSizeValidator implements FormalValidator<NotificaContext> {

    private final Integer getMaxPayments;

    @Override
    public ValidationResult validate(NotificaContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        checkMaxNumberOfPayments(context, errors);
        return new ValidationResult(errors);
    }

    private void checkMaxNumberOfPayments(NotificaContext context, ArrayList<ProblemError> errors) {
        context.getPayload().getRecipients()
                .forEach(
                        recipient -> {
                            if (recipient.getPayments() != null && recipient.getPayments().size() > getMaxPayments) {
                                errors.add(ProblemError.builder().element("payments").code(ErrorCodes.ERROR_CODE_MAX_PAYMENT.getValue()).detail("Max payment count reached for recipient with taxId: " + recipient.getTaxId()).build());
                            }
                        }
                );
    }
}
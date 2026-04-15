package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.context.NotificaContext;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
public class GroupValidator implements FormalValidator<NotificaContext> {


    @Override
    public ValidationResult validate(NotificaContext context) {

        ArrayList<ProblemError> errors = new ArrayList<>();
        return new ValidationResult(errors);
    }

}

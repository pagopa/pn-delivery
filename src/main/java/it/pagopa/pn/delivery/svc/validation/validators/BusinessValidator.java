package it.pagopa.pn.delivery.svc.validation.validators;

import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;

public interface BusinessValidator<C extends ValidationContext> extends Validator<C> {
     ValidationResult validate(C context);
}

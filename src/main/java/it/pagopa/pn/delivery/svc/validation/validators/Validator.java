package it.pagopa.pn.delivery.svc.validation.validators;

import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;

interface Validator<C extends ValidationContext> {
    ValidationResult validate(C ctx);
}
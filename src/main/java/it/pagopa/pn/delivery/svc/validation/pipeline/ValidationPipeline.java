package it.pagopa.pn.delivery.svc.validation.pipeline;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.exception.ValidationException;
import it.pagopa.pn.delivery.svc.validation.*;
import it.pagopa.pn.delivery.svc.validation.context.ValidationContext;
import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.validators.BusinessValidator;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import org.springframework.http.HttpStatus;

import java.util.List;

public final class ValidationPipeline<C extends ValidationContext> {

    private final List<AuthorizationValidator<? super C>> authorizationValidators;
    private final List<FormalValidator<? super C>> formalValidators;
    private final List<BusinessValidator<? super C>> businessValidators;

    ValidationPipeline(
            List<FormalValidator<? super C>> formalValidators,
            List<AuthorizationValidator<? super C>> authorizationValidators,
            List<BusinessValidator<? super C>> businessValidators
    ) {
        this.authorizationValidators = List.copyOf(authorizationValidators);
        this.formalValidators = List.copyOf(formalValidators);
        this.businessValidators = List.copyOf(businessValidators);
    }

    public static <C extends ValidationContext> ValidationPipelineBuilder<C> builder() {
        return new ValidationPipelineBuilder<>();
    }

    public void execute(C context) {
        runAuthorization(context);
        runFormal(context);
        runBusiness(context);
    }

    private void runAuthorization(C context) {
        authorizationValidators.stream()
                .map(v -> v.validate(context))
                .filter(ValidationResult::isFailure)
                .findFirst()
                .ifPresent(r -> {
                    throw new ValidationException(r.getErrors(), HttpStatus.FORBIDDEN.toString());
                });
    }

    private void runFormal(C context) {
        List<ProblemError> errors = formalValidators.stream()
                .map(v -> v.validate(context))
                .filter(ValidationResult::isFailure)
                .flatMap(r -> r.getErrors().stream())
                .toList();

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, HttpStatus.BAD_REQUEST.toString());
        }
    }

    private void runBusiness(C context) {
        List<ProblemError> errors = businessValidators.stream()
                .map(v -> v.validate(context))
                .filter(ValidationResult::isFailure)
                .flatMap(r -> r.getErrors().stream())
                .toList();

        if (!errors.isEmpty()) {
            throw new ValidationException(errors, HttpStatus.UNPROCESSABLE_ENTITY.toString());
        }
    }
}

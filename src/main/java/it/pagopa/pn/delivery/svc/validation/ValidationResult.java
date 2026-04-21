package it.pagopa.pn.delivery.svc.validation;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import lombok.Getter;

import java.util.List;

@Getter
public final class ValidationResult {

    private final List<ProblemError> errors;

    public ValidationResult(List<ProblemError> errors) {
        this.errors = List.copyOf(errors);
    }

    public static ValidationResult ok() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult failure(ProblemError error) {
        return new ValidationResult(List.of(error));
    }

    public static ValidationResult failure(List<ProblemError> errors) {
        if (errors.isEmpty()) throw new IllegalArgumentException(
                "Un failure deve avere almeno un errore"
        );
        return new ValidationResult(errors);
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public boolean isFailure() {
        return !errors.isEmpty();
    }
}
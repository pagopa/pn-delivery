package it.pagopa.pn.common.messages;

import it.pagopa.pn.api.dto.notification.Notification;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.Set;

public class PnValidationException extends IllegalArgumentException {

    private final Set<ConstraintViolation<Notification>> validationErrors;

    public PnValidationException( Set<ConstraintViolation<Notification>> validationErrors ) {
        super( validationErrors.toString() );
        this.validationErrors = Collections.unmodifiableSet( validationErrors );
    }

    public Set<ConstraintViolation<Notification>> getValidationErrors() {
        return validationErrors;
    }
}

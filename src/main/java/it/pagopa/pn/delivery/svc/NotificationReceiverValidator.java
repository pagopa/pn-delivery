package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.models.InternalNotification;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class NotificationReceiverValidator {

    private final Validator validator;

    public NotificationReceiverValidator(Validator validator) {
        this.validator = validator;
    }

    public void checkNewNotificationBeforeInsertAndThrow(InternalNotification internalNotification) {
        Set<ConstraintViolation<InternalNotification>> errors = checkNewNotificationBeforeInsert(internalNotification);
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(internalNotification.getPaProtocolNumber(), errors);
        }
    }

    public Set<ConstraintViolation<InternalNotification>> checkNewNotificationBeforeInsert(InternalNotification internalNotification) {
        return validator.validate( internalNotification );
    }

    public void checkNewNotificationRequestBeforeInsertAndThrow(NewNotificationRequest newNotificationRequest) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = checkNewNotificationRequestBeforeInsert(newNotificationRequest);
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(newNotificationRequest.getPaProtocolNumber(), errors);
        }
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationRequestBeforeInsert(NewNotificationRequest internalNotification) {
        return validator.validate( internalNotification );
    }

}

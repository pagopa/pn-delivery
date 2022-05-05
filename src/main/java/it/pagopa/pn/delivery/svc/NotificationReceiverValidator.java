package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.commons.exceptions.PnValidationException;
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
            throw new PnValidationException(internalNotification.getPaNotificationId(), errors);
        }
    }

    public Set<ConstraintViolation<InternalNotification>> checkNewNotificationBeforeInsert(InternalNotification internalNotification) {
        return validator.validate(internalNotification, NotificationJsonViews.New.class );
    }

    /*public void checkNewNotificationBeforeInsertAndThrow(NewNotificationRequest notification) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = checkNewNotificationBeforeInsert( notification );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(notification.getIdempotenceToken(), errors);
        }
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationBeforeInsert(NewNotificationRequest notification) {
        return validator.validate( notification, NewNotificationRequest.class );
    }*/
}

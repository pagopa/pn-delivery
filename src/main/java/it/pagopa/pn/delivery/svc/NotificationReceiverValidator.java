package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
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

    public void checkNewNotificationBeforeInsertAndThrow(Notification notification) {
        Set<ConstraintViolation<Notification>> errors = checkNewNotificationBeforeInsert( notification );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(notification.getPaNotificationId(), errors);
        }
    }

    public Set<ConstraintViolation<Notification>> checkNewNotificationBeforeInsert(Notification notification) {
        return validator.validate( notification, NotificationJsonViews.New.class );
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

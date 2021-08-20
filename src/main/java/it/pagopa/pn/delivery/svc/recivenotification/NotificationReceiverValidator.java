package it.pagopa.pn.delivery.svc.recivenotification;

import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.common.messages.PnValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class NotificationReceiverValidator {

    private final Validator validator;

    public NotificationReceiverValidator(Validator validator) {
        this.validator = validator;
    }

    public void checkNewNotificationBeforeInsertAndThrow(Notification notification) {
        Set<ConstraintViolation<Notification>> errors = checkNewNotificationBeforeInsert( notification );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException( errors );
        }
    }

    public Set<ConstraintViolation<Notification>> checkNewNotificationBeforeInsert(Notification notification) {
        return validator.validate( notification, NotificationJsonViews.New.class );
    }

}

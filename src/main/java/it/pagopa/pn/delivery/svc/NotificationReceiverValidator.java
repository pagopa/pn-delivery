package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.svc.preloaded_digest_error.DigestEqualityBean;
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
            throw new PnValidationException( errors );
        }
    }

    public Set<ConstraintViolation<Notification>> checkNewNotificationBeforeInsert(Notification notification) {
        return validator.validate( notification, NotificationJsonViews.New.class );
    }

    public void checkPreloadedDigests(String key, NotificationAttachment.Digests expected, NotificationAttachment.Digests actual) throws PnValidationException {
        Set<ConstraintViolation<DigestEqualityBean>> errors = validator.validate( DigestEqualityBean.builder()
                .key( key )
                .expected( expected )
                .actual( actual )
                .build()
            );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException( errors );
        }
    }
}

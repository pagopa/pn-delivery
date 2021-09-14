package it.pagopa.pn.delivery.svc.recivenotification;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
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

    public void checkNotificationAttachmentsDigestIsSha256(Notification notification){
        Set<ConstraintViolation<Notification>> errors = checkNotificationAttachmentsDigestIsSha256Encoded( notification );
        if( ! errors.isEmpty() ) {
            throw new PnValidationException( errors );
        }
    }

    private Set<ConstraintViolation<Notification>> checkNotificationAttachmentsDigestIsSha256Encoded(Notification notification){
        String sha256Str = null;
        Set<ConstraintViolation<Notification>> errors = Set.of();
        for(NotificationAttachment attachment : notification.getDocuments()){
            if (Base64.isBase64(attachment.getBody())) {
                byte[] base64Decoded = Base64.decodeBase64(attachment.getBody());
                sha256Str = DigestUtils.sha256Hex(base64Decoded);
            }
            if(sha256Str.isEmpty() || !attachment.getDigests().getSha256().equals(sha256Str)) {
                errors = validator.validate(notification, NotificationJsonViews.New.class);
            }
        }
        return errors;
    }
}

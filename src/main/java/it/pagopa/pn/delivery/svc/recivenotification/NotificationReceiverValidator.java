package it.pagopa.pn.delivery.svc.recivenotification;

import it.pagopa.pn.api.dto.notification.*;
import it.pagopa.pn.commons.exceptions.PnEncodingException;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigInteger;
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

    public boolean checkNotificationAttachmentsBodyIsBase64(Notification notification){
        for(NotificationAttachment attachment : notification.getDocuments()){
            if(!Base64.isBase64(attachment.getBody())) {
                throw new PnEncodingException();
            }
        }
        return true;
    }

    public boolean checkNotificationAttachmentsDigestIsSha256(Notification notification){
        String sha256Str;
        for(NotificationAttachment attachment : notification.getDocuments()){
            if (Base64.isBase64(attachment.getBody())) {
                byte[] base64Decoded = Base64.decodeBase64(attachment.getBody());
                sha256Str = DigestUtils.sha256Hex(base64Decoded);
            }
            else
                throw new PnEncodingException();

            if(!attachment.getDigests().getSha256().equals(sha256Str))
                throw new PnEncodingException();
        }
        return true;
    }

    private boolean checkF24AttachmentsAreBase64(Notification notification){

        NotificationAttachment flatRateAttachment = notification.getPayment().getF24().getFlatRate();
        NotificationAttachment digitalAttachment = notification.getPayment().getF24().getDigital();
        NotificationAttachment analogAttachment = notification.getPayment().getF24().getAnalog();

        if(!Base64.isBase64(flatRateAttachment.getBody()))
            throw new PnEncodingException();

        if(!Base64.isBase64(digitalAttachment.getBody()))
            throw new PnEncodingException();

        if(!Base64.isBase64(analogAttachment.getBody()))
            throw new PnEncodingException();

        return true;
    }
}

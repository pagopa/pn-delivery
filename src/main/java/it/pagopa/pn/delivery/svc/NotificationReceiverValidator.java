package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class NotificationReceiverValidator {

    private final Validator validator;
    private final PnDeliveryConfigs cfg;

    public NotificationReceiverValidator(Validator validator, PnDeliveryConfigs cfg) {
        this.validator = validator;
        this.cfg = cfg;
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
        Set<ConstraintViolation<NewNotificationRequest>> errors = checkNewNotificationRequestBeforeInsert( newNotificationRequest );
        if ( cfg.isMVPTrial() && errors.isEmpty() ) {
            errors = checkNewNotificationRequestForMVP( newNotificationRequest );
        }
        if( ! errors.isEmpty() ) {
            throw new PnValidationException(newNotificationRequest.getPaProtocolNumber(), errors);
        }
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationRequestBeforeInsert(NewNotificationRequest internalNotification) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = new HashSet<>();
        if ( internalNotification.getRecipients().size() > 1 ) {
            Set<String> distinctTaxIds = new HashSet<>();
            for (NotificationRecipient recipient : internalNotification.getRecipients() ) {
                if ( !distinctTaxIds.add( recipient.getTaxId() )){
                    ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Duplicated recipient taxId" );
                    errors.add( constraintViolation );
                }
            }
        }
        errors.addAll( validator.validate( internalNotification ));
        return errors;
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationRequestForMVP( NewNotificationRequest notificationRequest ) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = new HashSet<>();
        if ( !StringUtils.hasText(notificationRequest.getSenderDenomination()) ) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "No sender denomination" );
            errors.add( constraintViolation );
        }
        if ( notificationRequest.getRecipients().size() > 1 ) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Max one recipient" );
            errors.add( constraintViolation );
        }
        if ( cfg.isNotificationCheckAddress() && notificationRequest.getRecipients().get( 0 ).getPhysicalAddress() == null ) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "No recipient physical address" );
            errors.add( constraintViolation );
        }
        NotificationPaymentInfo payment = notificationRequest.getRecipients().get(0).getPayment();
        if (Objects.isNull( payment )) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "No recipient payment" );
            errors.add( constraintViolation );
        } else {
            String noticeCode = payment.getNoticeCode();
            String noticeCodeAlternative = payment.getNoticeCodeAlternative();
            if ( noticeCode.equals(noticeCodeAlternative) ) {
                ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Alternative notice code equals to notice code" );
                errors.add( constraintViolation );
            }
        }
        return errors;
    }

}

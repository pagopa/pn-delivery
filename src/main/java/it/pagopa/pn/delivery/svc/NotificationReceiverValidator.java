package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPaymentItem;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;

@Component
public class NotificationReceiverValidator {

    private final Validator validator;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final ValidateUtils validateUtils;
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public NotificationReceiverValidator(Validator validator, MVPParameterConsumer mvpParameterConsumer, ValidateUtils validateUtils, PnDeliveryConfigs pnDeliveryConfigs) {
        this.validator = validator;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.validateUtils = validateUtils;
        this.pnDeliveryConfigs = pnDeliveryConfigs;
    }

    public void checkNewNotificationBeforeInsertAndThrow(InternalNotification internalNotification) {
        Set<ConstraintViolation<InternalNotification>> errors = checkNewNotificationBeforeInsert(internalNotification);
        if( ! errors.isEmpty() ) {
            List<ProblemError> errorList  = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(internalNotification.getPaProtocolNumber(), errorList);
        }
    }

    public Set<ConstraintViolation<InternalNotification>> checkNewNotificationBeforeInsert(InternalNotification internalNotification) {
        return validator.validate( internalNotification );
    }

    public void checkNewNotificationRequestBeforeInsertAndThrow(NewNotificationRequest newNotificationRequest) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = checkNewNotificationRequestBeforeInsert( newNotificationRequest );
        if ( Boolean.TRUE.equals( mvpParameterConsumer.isMvp( newNotificationRequest.getSenderTaxId() )) && errors.isEmpty() ) {
            errors = checkNewNotificationRequestForMVP( newNotificationRequest );
        }
        if( ! errors.isEmpty() ) {
            List<ProblemError> errorList  = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(newNotificationRequest.getPaProtocolNumber(), errorList);
        }
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationRequestBeforeInsert(NewNotificationRequest internalNotification) {
      Set<ConstraintViolation<NewNotificationRequest>> errors = new HashSet<>();

      // check del numero massimo di documenti allegati
      if (pnDeliveryConfigs.getMaxAttachmentsCount() > 0 && internalNotification.getDocuments().size() > pnDeliveryConfigs.getMaxAttachmentsCount())
      {
          ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Max attachment count reached" );
          errors.add( constraintViolation );
          return errors;
      }

      // check del numero massimo di recipient
      if (pnDeliveryConfigs.getMaxRecipientsCount() > 0 && internalNotification.getRecipients().size() > pnDeliveryConfigs.getMaxRecipientsCount())
      {
          ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Max recipient count reached" );
          errors.add( constraintViolation );
          return errors;
      }

      int recIdx = 0;
      Set<String> distinctTaxIds = new HashSet<>();
      for (NotificationRecipient recipient : internalNotification.getRecipients() ) {
          if( !validateUtils.validate( recipient.getTaxId() ) ) {
              ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Invalid taxId for recipient " + recIdx );
              errors.add( constraintViolation );
          }
          if ( !distinctTaxIds.add( recipient.getTaxId() )){
              ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Duplicated recipient taxId" );
              errors.add( constraintViolation );
          }

          recIdx++;
      }
      errors.addAll(validator.validate( internalNotification ));
      return errors;
    }

    public Set<ConstraintViolation<NewNotificationRequest>> checkNewNotificationRequestForMVP( NewNotificationRequest notificationRequest ) {
        Set<ConstraintViolation<NewNotificationRequest>> errors = new HashSet<>();
       
        if ( notificationRequest.getRecipients().size() > 1 ) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "Max one recipient" );
            errors.add( constraintViolation );
        }

        NotificationPaymentItem payment = notificationRequest.getRecipients().get(0).getPayments().get(0);
        if (Objects.isNull( payment )) {
            ConstraintViolationImpl<NewNotificationRequest> constraintViolation = new ConstraintViolationImpl<>( "No recipient payment" );
            errors.add( constraintViolation );
        }
        return errors;
    }


}

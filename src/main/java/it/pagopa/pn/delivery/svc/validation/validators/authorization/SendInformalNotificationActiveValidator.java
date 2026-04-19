package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendInformalNotificationActiveValidator implements AuthorizationValidator<NotificationContext> {

    private final InformalNotificationSendPaParameterConsumer parameterConsumer;

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        checkIfCxIdIsMvp(context, errors);
        return new ValidationResult(errors);
    }

    private void checkIfCxIdIsMvp(NotificationContext context, ArrayList<ProblemError> errors) {
        if ( Boolean.FALSE.equals(parameterConsumer.isSenderActiveForInformalNotification(context.getCxId())) ) {
            errors.add( ProblemError.builder().code(ErrorCodes.ERROR_CODE_SEND_IS_DISABLED.getValue()).detail("Piattaforma Notifiche non é abilitata alla comunicazione di notifiche bonarie").build());
        }
    }
}

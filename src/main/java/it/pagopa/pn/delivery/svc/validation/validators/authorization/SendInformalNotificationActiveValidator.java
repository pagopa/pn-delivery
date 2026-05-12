package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.exception.ValidationException;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendInformalNotificationActiveValidator {
    private final InformalNotificationSendPaParameterConsumer parameterConsumer;

    public void validate(String xPagopaPnCxId) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        checkIfCxIdIsMvp(xPagopaPnCxId, errors);
        checkErrorList(errors);
    }

    private void checkErrorList(ArrayList<ProblemError> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors, "Validazione authorization non riuscita", HttpStatus.FORBIDDEN);
        }
    }

    private void checkIfCxIdIsMvp(String xPagopaPnCxId, ArrayList<ProblemError> errors) {
        if ( Boolean.FALSE.equals(parameterConsumer.isSenderActiveForInformalNotification(xPagopaPnCxId)) ) {
            errors.add( ProblemError.builder().code(ErrorCodes.ERROR_CODE_SEND_IS_DISABLED.getValue()).detail("Piattaforma Notifiche non è abilitata alla comunicazione di notifiche bonarie").build());
        }
    }
}

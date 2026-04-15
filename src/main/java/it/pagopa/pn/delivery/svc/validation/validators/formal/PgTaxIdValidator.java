package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV24;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificaContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class PgTaxIdValidator implements FormalValidator<NotificaContext> {
    
    @Override
    public ValidationResult validate(NotificaContext context) {
        int recIdx = 0;
        ArrayList<ProblemError> errors = new ArrayList<>();
        for (NotificationRecipient recipient : context.getPayload().getRecipients()) {
            checkTaxIdIsNumerical(recIdx, recipient, errors);
            recIdx++;
        }
        return new ValidationResult(errors);
    }

    private void checkTaxIdIsNumerical(int recIdx, NotificationRecipient recipient, ArrayList<ProblemError> errors) {
        if (NotificationRecipientV24.RecipientTypeEnum.PG.equals(recipient.getRecipientType()) &&
                (!recipient.getTaxId().matches("^\\d+$"))) {
            errors.add(ProblemError.builder().element("taxId").code(ErrorCodes.ERROR_CODE_PG_TAX_ID_NOT_NUMERICAL.getValue()).detail("SEND accepts only numerical taxId for PG recipient " + recIdx).build());
        }
    }
}

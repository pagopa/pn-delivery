package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
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
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecipientTaxIdSyntaxValidator implements FormalValidator<NotificaContext> {

    private final ValidateUtils validateUtils;
    private final PnDeliveryConfigs pnDeliveryConfigs;

    @Override
    public ValidationResult validate(NotificaContext context) {
        int recIdx = 0;
        Set<String> distinctTaxIds = new HashSet<>();
        ArrayList<ProblemError> errors = new ArrayList<>();
        for (NotificationRecipient recipient : context.getPayload().getRecipients()) {

            // limitazione temporanea: destinatari PG possono avere solo TaxId numerico
            onlyNumericalTaxIdForPGV2(recIdx, recipient, errors);
            //Check taxId
            checkTaxId(recipient, distinctTaxIds, recIdx, errors);

            recIdx++;
        }

        return new ValidationResult(errors);
    }

    private void onlyNumericalTaxIdForPGV2(int recIdx, NotificationRecipient recipient, ArrayList<ProblemError> errors) {
        if (NotificationRecipientV24.RecipientTypeEnum.PG.equals(recipient.getRecipientType()) &&
                (!recipient.getTaxId().matches("^\\d+$"))) {
            errors.add(ProblemError.builder().element("taxId").code(ErrorCodes.ERROR_CODE_PG_TAX_ID_NOT_NUMERICAL.getValue()).detail("SEND accepts only numerical taxId for PG recipient " + recIdx).build());
        }
    }

    /**
     * Validazio di NotificationRecipientV24 per verificare il taxId e la sua univocità
     * @param recipient
     * @return
     */
    protected void checkTaxId(NotificationRecipient recipient, Set<String> distinctTaxIds, int recIdx, ArrayList<ProblemError> errors){
        boolean isPF = NotificationRecipientV24.RecipientTypeEnum.PF.getValue().equals(recipient.getRecipientType().getValue());
        boolean skipCheckTaxIdInBlackList = pnDeliveryConfigs.isSkipCheckTaxIdInBlackList();

        if( !validateUtils.validate(recipient.getTaxId(), isPF, false, skipCheckTaxIdInBlackList)) {
            errors.add(ProblemError.builder().element("taxId").code(ErrorCodes.ERROR_CODE_INVALID_TAX_ID.getValue()).detail("Invalid taxId for recipient " + recIdx ).build());
        }

        if ( !distinctTaxIds.add( recipient.getTaxId() )){
            errors.add(ProblemError.builder().element("taxId").code(ErrorCodes.ERROR_CODE_DUPLICATED_RECIPIENT_TAX_ID.getValue()).detail("Duplicated recipient taxId").build());
        }
    }
}

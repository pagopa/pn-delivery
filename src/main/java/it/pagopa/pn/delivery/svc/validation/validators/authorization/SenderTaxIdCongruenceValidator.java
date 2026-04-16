package it.pagopa.pn.delivery.svc.validation.validators.authorization;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaInfo;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.validators.AuthorizationValidator;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_PA_NOT_FOUND;

@Component
@Slf4j
@RequiredArgsConstructor
public class SenderTaxIdCongruenceValidator implements AuthorizationValidator<NotificationContext> {

    private final PnDeliveryConfigs pnDeliveryConfigs;
    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();

        checkSenderTaxIdCongruence(context, errors);

        return new ValidationResult(errors);
    }

    private void checkSenderTaxIdCongruence(NotificationContext context, ArrayList<ProblemError> errors) {
        if(pnDeliveryConfigs.isEnableSenderTaxIdCongruence()){
            PaInfo paInfo = pnExternalRegistriesClient.getOnePa(context.getCxId());
            if (paInfo == null || !StringUtils.hasText(paInfo.getTaxId())) {
                throw new PnInternalException("Unable to retrieve PA taxId from external registries", 500, ERROR_CODE_DELIVERY_PA_NOT_FOUND);
            }

            if(!context.getPayload().getSenderTaxId().equals(paInfo.getTaxId())){
                errors.add( ProblemError.builder().element("cxId").code(ErrorCodes.ERROR_CODE_INVALID_TAX_ID.getValue()).detail("Il codice fiscale del mittente risulta incongruente").build());
            }
        }
    }
}

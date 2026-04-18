package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class CampaignMessageLanguageValidator implements FormalValidator<InformalNotificationContext> {

    private final Boolean isInformalNotificationCheckCampaignLangActive;

    @Override
    public ValidationResult validate(InformalNotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();

        checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(context, errors);

        return new ValidationResult(errors);
    }

    private void checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(InformalNotificationContext context, ArrayList<ProblemError> errors) {

        if (!isInformalNotificationCheckCampaignLangActive) {
            log.debug("Informal notification - check campaign language is disabled, skipping validation");
            return;
        }

        if(Objects.isNull(context.getCampaign())) {
            throw new PnInternalException("Campaign is required in the context for CampaignMessageLanguageValidator", ErrorCodes.ERROR_CODE_INVALID_CONTEXT.getValue());
        }

        List<String> languagesInCampaign = context.getCampaign().getMessages().stream().map(message -> message.getAdditionalLanguage().name()).toList();
        List<String> requestedAdditionalLanguages = context.getPayload().getAdditionalLanguages() != null ? context.getPayload().getAdditionalLanguages() : Collections.emptyList();

        if (!new HashSet<>(languagesInCampaign).containsAll(requestedAdditionalLanguages)) {
            errors.add( ProblemError.builder().element("additionalLanguages").code(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue()).detail("All requested additional languages must be present in the selected campaign.").build());
        }
    }

}

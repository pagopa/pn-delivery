package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificaContext;
import it.pagopa.pn.delivery.svc.validation.context.NotificaInformaleContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CampaignMessageLanguageValidator implements FormalValidator<NotificaContext> {

    private final Boolean isInformalNotificationCheckCampaignLangActive;

    @Override
    public ValidationResult validate(NotificaContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();

        NotificaInformaleContext internalContext = (NotificaInformaleContext) context;

        checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(internalContext, errors);

        return new ValidationResult(errors);
    }

    private void checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(NotificaInformaleContext context, ArrayList<ProblemError> errors) {

        if (!isInformalNotificationCheckCampaignLangActive) {
            return;
        }

        List<String> languagesInCampaign = context.getCampaign().getMessagesId().stream().map(message -> message.getAdditionalLanguage().name()).toList();
        List<String> requestedAdditionalLanguages = context.getAdditionalLanguages();

        if (!new HashSet<>(languagesInCampaign).containsAll(requestedAdditionalLanguages)) {
            errors.add( ProblemError.builder().element("additionalLanguages").code(ErrorCodes.ERROR_CODE_INVALID_ADDITIONAL_LANG.getValue()).detail("All requested additional languages must be present in the current campaign.").build());
        }
    }

}

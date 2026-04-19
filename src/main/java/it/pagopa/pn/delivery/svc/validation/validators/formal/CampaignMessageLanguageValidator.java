package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static it.pagopa.pn.delivery.utils.InformalNotificationUtils.findMessageIdInCampaign;

@Slf4j
@RequiredArgsConstructor
public class CampaignMessageLanguageValidator implements FormalValidator<InformalNotificationContext> {

    private final Boolean isInformalNotificationCheckCampaignLangActive;

    @Override
    public ValidationResult validate(InformalNotificationContext context) {
        if(Objects.isNull(context.getCampaign())) {
            throw new PnInternalException("Campaign is required in the context for CampaignMessageLanguageValidator", ErrorCodes.ERROR_CODE_INVALID_CONTEXT.getValue());
        }

        ArrayList<ProblemError> errors = new ArrayList<>();

        checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(context, errors);

        return new ValidationResult(errors);
    }

    private void checkCongruenceBetweenAdditionalLanguageRequestedAndCampaign(InformalNotificationContext context, ArrayList<ProblemError> errors) {
        if (!isInformalNotificationCheckCampaignLangActive) {
            log.debug("Informal notification - check campaign language is disabled, skipping validation");
            return;
        }

        InternalNotification notification = context.getPayload();
        boolean existsRecipientWithoutMessageId = notification.getRecipients().stream().anyMatch(recipient -> recipient.getMessageId() == null);

        if(!existsRecipientWithoutMessageId) {
            log.debug("Informal notification - all recipients have messageId, skipping campaign language validation");
            return;
        }

        Optional<String> messageIdOpt = findMessageIdInCampaign(notification.getAdditionalLanguages(), context.getCampaign().getMessages());
        if (messageIdOpt.isEmpty()) {
            errors.add( ProblemError.builder().element("additionalLanguages").code(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue()).detail("The referenced campaign contains no messages matching the selected language configuration.").build());
        }
    }

}

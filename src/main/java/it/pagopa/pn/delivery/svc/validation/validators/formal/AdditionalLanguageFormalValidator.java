package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdditionalLanguageFormalValidator implements FormalValidator<NotificationContext> {

    private static final String REQUIRED_ADDITIONAL_LANG_SIZE = "È obbligatorio fornire una sola lingua aggiuntiva.";

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();

        checkAdditionalLanguages(context.getPayload(), errors);

        return new ValidationResult(errors);
    }

    private void checkAdditionalLanguages(InternalNotification notification, ArrayList<ProblemError> errors) {
        List<String> additionalLanguages = notification.getAdditionalLanguages();
        if(!CollectionUtils.isNullOrEmpty(additionalLanguages) && additionalLanguages.size() > 1){
            errors.add( ProblemError.builder().element("additionalLanguages").code(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_MAX_SIZE_EXCEEDED.getValue()).detail(REQUIRED_ADDITIONAL_LANG_SIZE).build());
        }
        else if(!CollectionUtils.isNullOrEmpty(additionalLanguages) && !isValidAdditionalLanguage(additionalLanguages.get(0))){
            String logMessage = String.format("Lingua aggiuntiva non valida, i valori accettati sono %s", Arrays.stream(AllowedAdditionalLanguages.values()).map(Enum::name).collect(Collectors.joining(",")));
            errors.add( ProblemError.builder().element("additionalLanguages").code(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue()).detail(logMessage).build());
        }
    }

    private boolean isValidAdditionalLanguage(String lang) {
        return Arrays.stream(AllowedAdditionalLanguages.values())
                .map(AllowedAdditionalLanguages::name)
                .anyMatch(lang::equals);
    }

}

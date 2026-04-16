package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import it.pagopa.pn.delivery.utils.DenominationValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.NONE;
import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.REGEX;

@Slf4j
@RequiredArgsConstructor
public class DenominationAndAtValidator implements FormalValidator<NotificationContext> {

    private final Integer denominationLength;
    private final String denominationValidationTypeValue;
    private final String denominationValidationRegexValue;
    private final String denominationValidationExcludedCharacter;

    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();
        int recIdx = 0;

        for (NotificationRecipient recipient : context.getPayload().getRecipients()) {
            checkDenomination(recipient, recIdx, errors);
            recIdx++;
        }

        return new ValidationResult(errors);
    }

    private record ValidationRegex(String regex, String excludedCharacterRegex){}

    private void checkDenomination(NotificationRecipient recipient, int recIdx, ArrayList<ProblemError> errors) {

        Pair<String, String> denomination = Pair.of("denomination", recipient.getDenomination());
        ArrayList<Pair<String, String>> fieldsToCheck = new ArrayList<>();
        fieldsToCheck.add(denomination);

        Pair<String, String> at;
        String atValue = recipient.getPhysicalAddress() == null ? null : recipient.getPhysicalAddress().getAt();
        if (atValue != null && !atValue.isEmpty()) {
            at = Pair.of("at", atValue);
            fieldsToCheck.add(at);
        }

        if(denominationLength != null && denominationLength != 0){
            fieldsToCheck.stream()
                    .filter(field -> field.getValue() != null && field.getValue().trim().length() > denominationLength )
                    .map(field -> ProblemError.builder().detail(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), recIdx, denominationLength)).element("denomination").code(ErrorCodes.ERROR_CODE_DENOMINATION_LENGTH_EXCEEDED.getValue()).build())
                    .forEach(errors::add);
        }

        if(denominationValidationTypeValue != null && !denominationValidationTypeValue.equalsIgnoreCase(NONE.name())){
            String denominationValidationType = denominationValidationTypeValue.toLowerCase();

            ValidationRegex validationRegex = initializeValidationRegex(denominationValidationType);

            String regex = validationRegex.regex;
            String excludeCharacterRegex = validationRegex.excludedCharacterRegex;

            log.info("Check denomination/at with validation type {}",denominationValidationType);
            fieldsToCheck.stream()
                    .filter(field -> filterDenomination(field,regex,excludeCharacterRegex))
                    .map(field -> ProblemError.builder().element("denomination").detail(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), recIdx)).code(ErrorCodes.ERROR_CODE_DENOMINATION_INVALID_CHARACTERS.getValue()).build())
                    .forEach(errors::add);
        }
    }

    private ValidationRegex initializeValidationRegex(String denominationValidationType){
        String regex;
        String excludeCharacterRegex = null;
        if(denominationValidationType.equalsIgnoreCase(REGEX.name() )){
            regex ="[" + denominationValidationRegexValue + "]*";
        }else{
            regex = DenominationValidationUtils.getRegexValue(denominationValidationType);

            if(denominationValidationExcludedCharacter != null &&
                    !denominationValidationExcludedCharacter.trim().isEmpty() &&
                    !denominationValidationExcludedCharacter.equalsIgnoreCase(NONE.name())){
                excludeCharacterRegex = "[^"+denominationValidationExcludedCharacter+"]*";
            }
        }
        return new ValidationRegex(regex,excludeCharacterRegex);
    }

    private boolean filterDenomination(Pair<String,String> field, String regex, String excludeCharacterRegex ){
        return field.getValue() != null
                && ((!field.getValue().matches(regex))
                || (excludeCharacterRegex != null && (!field.getValue().matches(excludeCharacterRegex))));
    }

}

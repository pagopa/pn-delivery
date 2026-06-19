package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidateNotificationUtils {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    public static void validateInputSenderReceiver(InputSearchNotificationDto searchDto) {
        Set<ConstraintViolation<InputSearchNotificationDto>> errors = VALIDATOR.validate(searchDto);
        if (!errors.isEmpty()) {
            log.error("Validation search input ERROR {} - senderReceiverId {}", errors, searchDto.getSenderReceiverId());
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(searchDto.getSenderReceiverId(), errorList);
        }
        log.debug("Validation search input OK - senderReceiverId {}", searchDto.getSenderReceiverId());
    }

    public static void validateInputDelegate(InputSearchNotificationDelegatedDto searchDto) {
        Set<ConstraintViolation<InputSearchNotificationDelegatedDto>> errors = VALIDATOR.validate(searchDto);
        if (!errors.isEmpty()) {
            log.error("validation search input failed - delegateId {} - errors: {}", searchDto.getDelegateId(), errors);
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(searchDto.getDelegateId(), errorList);
        }
        log.debug("validation search input succeeded - delegateId {}", searchDto.getDelegateId());
    }
}

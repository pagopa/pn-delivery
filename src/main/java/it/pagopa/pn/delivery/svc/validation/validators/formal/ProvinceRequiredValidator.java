package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProvinceRequiredValidator implements FormalValidator<NotificationContext> {


    @Override
    public ValidationResult validate(NotificationContext context) {
        ArrayList<ProblemError> errors = new ArrayList<>();

        for (NotificationRecipient recipient : context.getPayload().getRecipients()) {
            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            checkIfProvinceIsRequired(physicalAddress, errors);
        }

        return new ValidationResult(errors);
    }

    private void checkIfProvinceIsRequired(NotificationPhysicalAddress physicalAddress, ArrayList<ProblemError> errors) {
        if (Objects.isNull(physicalAddress)) return;

        boolean addressIsItalian = !StringUtils.hasText(physicalAddress.getForeignState()) || physicalAddress.getForeignState().toUpperCase().trim().startsWith("ITAL");
        boolean addressDoesNotHaveProvince = !StringUtils.hasText(physicalAddress.getProvince());

        if (addressIsItalian && addressDoesNotHaveProvince) {
            errors.add(ProblemError.builder()
                    .element("address")
                    .code(ErrorCodes.ERROR_CODE_PROVINCE_REQUIRED.getValue())
                    .detail("No province provided in physical address")
                    .build());
        }
    }
}

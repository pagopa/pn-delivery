package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP;

@Component
@Slf4j
@RequiredArgsConstructor
public class GroupValidator implements FormalValidator<NotificationContext> {
    private final PnExternalRegistriesClientImpl pnExternalRegistriesClient;
    private static final String FIELD_NAME = "group";

    @Override
    public ValidationResult validate(NotificationContext context) {

        ArrayList<ProblemError> errors = new ArrayList<>();
        String notificationGroup = context.getPayload().getGroup();
        String senderId = context.getCxId();
        List<String> xPagopaPnCxGroups = context.getCxGroups();

        checkGroup(errors, notificationGroup, senderId, xPagopaPnCxGroups);
        return new ValidationResult(errors);
    }

    private void checkGroup(ArrayList<ProblemError> errors, String notificationGroup, String senderId, List<String> xPagopaPnCxGroups) {
        if( StringUtils.hasText( notificationGroup ) ) {

            List<PaGroup> paGroups = pnExternalRegistriesClient.getGroups( senderId, true );
            PaGroup paGroup = paGroups.stream().filter(
                    elem -> elem.getId() != null && elem.getId().equals(notificationGroup))
                    .findAny()
                    .orElse(null);

            if( paGroup == null ){
                String detailMessage = String.format("Group=%s not present or suspended/deleted in pa_groups=%s", notificationGroup, paGroups);
                errors.add(ProblemError.builder().element(FIELD_NAME).detail(detailMessage).code(ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP).build());
            }

            if( !CollectionUtils.isEmpty( xPagopaPnCxGroups ) && !xPagopaPnCxGroups.contains(notificationGroup)){
                String detailMessage = String.format("Group=%s not present in cx_groups=%s", notificationGroup, xPagopaPnCxGroups);
                errors.add(ProblemError.builder().element(FIELD_NAME).detail(detailMessage).code(ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP).build());
            }
        } else {
            if( !CollectionUtils.isEmpty( xPagopaPnCxGroups ) ) {
                String detailMessage = String.format( "Specify a group in cx_groups=%s", xPagopaPnCxGroups );
                errors.add(ProblemError.builder().element(FIELD_NAME).detail(detailMessage).code(ERROR_CODE_DELIVERY_INVALIDPARAMETER_GROUP).build());
            }
        }
    }

}

package it.pagopa.pn.delivery.svc.validation;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import it.pagopa.pn.delivery.svc.util.LanguageUtils;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;

import java.util.Arrays;

public class InformalMessageValidator {
    private InformalMessageValidator() {}

    public static void validate(NewMessageRequest request, PnDeliveryConfigs pnDeliveryConfigs) {
        if (request == null || request.getPrimaryMessage() == null) {
            throw new PnBadRequestException("Primary message is required", "Primary message is required", PnDeliveryExceptionCodes.ERROR_CODE_INFORMAL_PRIMARY_MESSAGE_REQUIRED);
        }
        var primary = request.getPrimaryMessage();
        if (!"IT".equalsIgnoreCase(primary.getLanguage())) {
            throw new PnBadRequestException("Primary message language must be IT", "Primary message language must be IT", PnDeliveryExceptionCodes.ERROR_CODE_INFORMAL_PRIMARY_LANGUAGE_NOT_IT);
        }
        if (request.getAdditionalMessage() != null) {
            String addLang = request.getAdditionalMessage().getLanguage();
            if (addLang == null || !LanguageUtils.isValidAdditionalLanguage(addLang)) {
                String accepted = String.join(", ", Arrays.stream(AllowedAdditionalLanguages.values()).map(Enum::name).toArray(String[]::new));
                throw new PnBadRequestException("Additional message language must be one of: " + accepted, "Additional message language must be one of: " + accepted, PnDeliveryExceptionCodes.ERROR_CODE_INFORMAL_INVALID_ADDITIONAL_LANGUAGE);
            }
        }
        // Validazione: somma lunghezze body
        int longBodyLen = 0;
        int shortBodyLen = 0;
        longBodyLen += primary.getLongBody() != null ? primary.getLongBody().length() : 0;
        if (primary.getShortBody() != null)
            shortBodyLen += primary.getShortBody().length();
        if (request.getAdditionalMessage() != null) {
            var secondary = request.getAdditionalMessage();
            longBodyLen += secondary.getLongBody() != null ? secondary.getLongBody().length() : 0;
            if (secondary.getShortBody() != null)
                shortBodyLen += secondary.getShortBody().length();
        }
        if ((pnDeliveryConfigs.getMaxMessageLongBodyLength() != null && longBodyLen > pnDeliveryConfigs.getMaxMessageLongBodyLength()) ||
            (pnDeliveryConfigs.getMaxMessageShortBodyLength() != null && shortBodyLen > pnDeliveryConfigs.getMaxMessageShortBodyLength())) {
            throw new PnBadRequestException("Body length exceeds max allowed", "Body length exceeds max allowed", PnDeliveryExceptionCodes.ERROR_CODE_INFORMAL_BODY_LENGTH_EXCEEDED);
        }
    }
}

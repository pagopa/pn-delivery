package it.pagopa.pn.delivery.svc.validation;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import it.pagopa.pn.delivery.svc.util.LanguageUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public class InformalMessageValidator {
    private InformalMessageValidator() {}

    public static void validate(MessageRequestDto requestDto, PnDeliveryConfigs pnDeliveryConfigs) {
        if (!"IT".equalsIgnoreCase(requestDto.getPrimaryContent().getLanguage().getValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary message language must be IT");
        }
        if (requestDto.getSecondaryContent() != null) {
            var languageEnum = requestDto.getSecondaryContent().getLanguage();
            String addLang = languageEnum.getValue();
            if (addLang == null || !LanguageUtils.isValidAdditionalLanguage(addLang)) {
                String accepted = String.join(", ", Arrays.stream(AllowedAdditionalLanguages.values()).map(Enum::name).toArray(String[]::new));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Additional message language must be one of: " + accepted);
            }
        }
        // Validazione: somma lunghezze body
        int longBodyLen = 0;
        int shortBodyLen = 0;
        longBodyLen += requestDto.getPrimaryContent().getLongBody().length();
        if (requestDto.getPrimaryContent().getShortBody() != null)
            shortBodyLen += requestDto.getPrimaryContent().getShortBody().length();
        if (requestDto.getSecondaryContent() != null) {
            longBodyLen += requestDto.getSecondaryContent().getLongBody().length();
            if (requestDto.getSecondaryContent().getShortBody() != null)
                shortBodyLen += requestDto.getSecondaryContent().getShortBody().length();
        }
        if ((pnDeliveryConfigs.getMaxMessageLongBodyLength() != null && longBodyLen > pnDeliveryConfigs.getMaxMessageLongBodyLength()) ||
            (pnDeliveryConfigs.getMaxMessageShortBodyLength() != null && shortBodyLen > pnDeliveryConfigs.getMaxMessageShortBodyLength())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body length exceeds max allowed");
        }
    }
}

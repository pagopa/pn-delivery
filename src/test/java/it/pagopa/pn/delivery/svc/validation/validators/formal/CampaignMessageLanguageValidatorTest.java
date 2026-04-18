package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.campaign.Message;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignMessageLanguageValidatorTest {

    @Test
    void shouldReturnSuccessWhenFeatureIsDisabled() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(false);

        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("SL")), campaign(Message.AdditionalLanguage.DE)));

        assertSuccess(result);
    }

    @Test
    void shouldThrowExceptionWhenCampaignIsMissing() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(true);

        Assertions.assertThrows(PnInternalException.class, () -> validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("SL")), null)));
    }

    @Test
    void shouldReturnSuccessWhenRequestedLanguageIsPresentInCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(true);

        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("DE")), campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenRequestedLanguageIsMissingFromCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(true);

        ValidationResult result = validator.validate(informalContext(buildNotificationWithAdditionalLanguages(List.of("SL")), campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("must be present in the selected campaign");
    }

    private InternalNotification buildNotificationWithAdditionalLanguages(List<String> additionalLanguages) {
        return InternalNotification.builder()
                .additionalLanguages(additionalLanguages)
                .build();
    }
}


package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.campaign.Message;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.campaign;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.informalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignMessageLanguageValidatorTest {

    @Test
    void shouldReturnSuccessWhenFeatureIsDisabled() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(false);

        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), List.of("SL"), campaign(Message.AdditionalLanguage.DE)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnSuccessWhenRequestedLanguageIsPresentInCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(true);

        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), List.of("DE"), campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenRequestedLanguageIsMissingFromCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator(true);

        ValidationResult result = validator.validate(informalContext(notification(List.of(), List.of()), List.of("SL"), campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_INVALID_ADDITIONAL_LANG.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("must be present in the current campaign");
    }
}


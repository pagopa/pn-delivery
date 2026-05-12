package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import it.pagopa.pn.delivery.models.campaign.Message;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.*;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.informalContext;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignMessageLanguageValidatorTest {
    @Test
    void shouldThrowExceptionWhenCampaignIsMissing() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator();
        InternalNotification notification = buildNotification(List.of("SL"), null);
        InformalNotificationContext context = informalContext(notification, null);
        Assertions.assertThrows(PnInternalException.class, () -> validator.validate(context));
    }

    @Test
    void shouldReturnSuccessWhenNotificationHasAllRecipientsWithMessageId() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator();
        InternalNotification notification = buildNotification(List.of("DE"), buildRecipients(true));
        ValidationResult result = validator.validate(informalContext(notification, campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnSuccessWhenRequestedLanguageIsPresentInCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator();
        InternalNotification notification = buildNotification(List.of("DE"), buildRecipients(false));

        ValidationResult result = validator.validate(informalContext(notification, campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertSuccess(result);
    }

    @Test
    void shouldReturnErrorWhenRecipientHasNotMessageIdButCampaignHasNoMessages() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator();
        InternalNotification notification = buildNotification(List.of("SL"), buildRecipients(false));
        ValidationResult result = validator.validate(informalContext(notification, new Campaign()));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("The referenced campaign contains no messages matching the selected language configuration");
    }

    @Test
    void shouldReturnErrorWhenRequestedLanguageIsMissingFromCampaign() {
        CampaignMessageLanguageValidator validator = new CampaignMessageLanguageValidator();
        InternalNotification notification = buildNotification(List.of("SL"), buildRecipients(false));
        ValidationResult result = validator.validate(informalContext(notification, campaign(Message.AdditionalLanguage.DE, Message.AdditionalLanguage.FR)));

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_ADDITIONAL_LANG_UNSUPPORTED_VALUE.getValue());
        assertThat(result.getErrors().get(0).getDetail()).contains("The referenced campaign contains no messages matching the selected language configuration");
    }

    private List<NotificationRecipient> buildRecipients(boolean includeMessageId) {
        return List.of(
                NotificationRecipient.builder().messageId(includeMessageId ? "test-message-id" : null).build()
        );
    }

    private InternalNotification buildNotification(List<String> additionalLanguages, List<NotificationRecipient> recipients) {
        return InternalNotification.builder()
                .additionalLanguages(additionalLanguages)
                .recipients(recipients)
                .build();
    }
}


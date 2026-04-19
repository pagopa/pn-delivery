package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.models.campaign.Message;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InformalNotificationUtilsTest {
    @Test
    void findMessageIdWhenAdditionalLanguageMatches() {
        List<String> additionalLanguages = List.of("FR");
        List<Message> campaignMessages = List.of(
                buildMessage("message001", "FR"),
                buildMessage("message002", "DE")
        );

        Optional<String> result = InformalNotificationUtils.findMessageIdInCampaign(additionalLanguages, campaignMessages);

        assertTrue(result.isPresent());
        assertEquals("message001", result.get());
    }

    @Test
    void findMessageIdWhenAdditionalLanguageIsNull() {
        List<String> additionalLanguages = null;
        List<Message> campaignMessages = List.of(
                buildMessage("message001", null),
                buildMessage("message002", "DE")
        );

        Optional<String> result = InformalNotificationUtils.findMessageIdInCampaign(additionalLanguages, campaignMessages);

        assertTrue(result.isPresent());
        assertEquals("message001", result.get());
    }

    @Test
    void findMessageIdWhenNoMatchingAdditionalLanguage() {
        List<String> additionalLanguages = List.of("IT");
        List<Message> campaignMessages = List.of(
                buildMessage("message001", "FR"),
                buildMessage("message002", "DE")
        );

        Optional<String> result = InformalNotificationUtils.findMessageIdInCampaign(additionalLanguages, campaignMessages);

        assertFalse(result.isPresent());
    }

    @Test
    void findMessageIdWhenCampaignMessagesIsEmpty() {
        List<String> additionalLanguages = List.of("FR");
        List<Message> campaignMessages = List.of();

        Optional<String> result = InformalNotificationUtils.findMessageIdInCampaign(additionalLanguages, campaignMessages);

        assertFalse(result.isPresent());
    }

    @Test
    void findMessageIdWhenAdditionalLanguagesIsEmpty() {
        List<String> additionalLanguages = List.of();
        List<Message> campaignMessages = List.of(
                buildMessage("message001", null),
                buildMessage("message002", "DE")
        );

        Optional<String> result = InformalNotificationUtils.findMessageIdInCampaign(additionalLanguages, campaignMessages);

        assertTrue(result.isPresent());
        assertEquals("message001", result.get());
    }

    private Message buildMessage(String messageId, String additionalLanguage) {
        return Message.builder()
                .messageId(messageId)
                .additionalLanguage(additionalLanguage != null ? Message.AdditionalLanguage.valueOf(additionalLanguage) : null)
                .build();
    }
}
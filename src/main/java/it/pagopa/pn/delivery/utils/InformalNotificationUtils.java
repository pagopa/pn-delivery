package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.models.campaign.Message;

import java.util.List;
import java.util.Optional;

public class InformalNotificationUtils {
    private InformalNotificationUtils() {
    }

    public static Optional<String> findMessageIdInCampaign(List<String> additionalLanguages, List<Message> campaignMessages) {
        if(campaignMessages == null || campaignMessages.isEmpty()) {
            return Optional.empty();
        }

        String requestedAdditionalLanguage = additionalLanguages != null && !additionalLanguages.isEmpty() ? additionalLanguages.get(0) : null;

        return campaignMessages.stream()
                .filter(msg -> {
                    if (requestedAdditionalLanguage == null) {
                        return msg.getAdditionalLanguage() == null;
                    } else {
                        return requestedAdditionalLanguage.equals(msg.getAdditionalLanguage().name());
                    }
                })
                .map(Message::getMessageId)
                .findFirst();
    }
}

package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
public class CampaignsParameterConsumer {

    private static final String PARAMETER_STORE_MVP_CAMPAIGNS = "MVPCampaigns";

    private final ParameterConsumer parameterConsumer;

    public CampaignsParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    public List<Campaign> getCampaignsBySenderId(String senderId) {
        Optional<Campaign[]> maybeCampaigns = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MVP_CAMPAIGNS,
                Campaign[].class
        );

        if (maybeCampaigns.isEmpty()) {
            log.debug("No campaign configuration found on parameter store");
            return Collections.emptyList();
        }

        return Arrays.stream(maybeCampaigns.get())
                .filter(campaign -> senderId.equals(campaign.getSenderId()))
                .toList();
    }

    public Campaign getCampaignByCampaignIdAndSenderId(String campaignId, String senderId) {
        return getCampaignsBySenderId(senderId).stream()
                .filter(campaign -> campaignId.equals(campaign.getCampaignId()))
                .findFirst()
                .orElseThrow(() -> new PnCampaignNotFoundException(
                        String.format("Campaign with campaignId=%s and senderId=%s not found", campaignId, senderId)
                ));
    }
}


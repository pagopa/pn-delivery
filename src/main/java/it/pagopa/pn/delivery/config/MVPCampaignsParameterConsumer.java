package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignException;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MVPCampaignsParameterConsumer {

    private final ParameterConsumer parameterConsumer;

    private static final String PARAMETER_STORE_MVP_CAMPAIGNS = "MVPCampaigns";

    public Campaign getCampaignByCampaignIdAndSenderId(String campaignId, String senderId ) {
        log.debug( "Start getCampaignByCampaignIdAndSenderId for campaignId={} and senderId={}", campaignId, senderId );

        Optional<Campaign[]> optionalMVPCampaigns = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MVP_CAMPAIGNS, Campaign[].class);
        if( optionalMVPCampaigns.isPresent() ) {
            Campaign[] mvpCampaigns = optionalMVPCampaigns.get();
            for (Campaign mvpCampaign : mvpCampaigns ) {
                if ( mvpCampaign.getCampaignId().equals(campaignId) && mvpCampaign.getSenderId().equals(senderId) ) {
                    return mvpCampaign;
                }
            }
        }

        throw new PnCampaignException("Campaign not found", String.format("Campaign with campaignId=%s and senderId=%s not found", campaignId, senderId), "campaign_not_found");
    }
}

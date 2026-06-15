package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Configuration
public class CampaignsParameterConsumer {

    private static final String PARAMETER_STORE_MVP_CAMPAIGNS = "MVPCampaigns";

    private final ParameterConsumer parameterConsumer;
    private List<Campaign> campaigns = Collections.emptyList();

    public CampaignsParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    @PostConstruct
    protected void initialize() {
        Optional<Campaign[]> maybeCampaigns = parameterConsumer.getParameterValue(
                PARAMETER_STORE_MVP_CAMPAIGNS,
                Campaign[].class
        );

        if (maybeCampaigns.isEmpty()) {
            log.info("No campaign configuration found on parameter store");
            return;
        }

        List<Campaign> loaded = new ArrayList<>();
        for (Campaign campaign : maybeCampaigns.get()) {
            if (isValid(campaign)) {
                loaded.add(campaign);
            } else {
                log.warn("Invalid campaign configuration found: {}", campaign);
            }
        }
        campaigns = Collections.unmodifiableList(loaded);

        log.info("Loaded {} campaigns in memory", campaigns.size());
    }

    public List<Campaign> getCampaignsBySenderId(String senderId) {
        return campaigns.stream()
                .filter(campaign -> Objects.equals(senderId, campaign.getSenderId()))
                .toList();
    }

    public Campaign getCampaignByCampaignIdAndSenderId(String campaignId, String senderId) {
        return campaigns.stream()
                .filter(campaign -> Objects.equals(campaignId, campaign.getCampaignId())
                        && Objects.equals(senderId, campaign.getSenderId()))
                .findFirst()
                .orElseThrow(() -> new PnCampaignNotFoundException(
                        String.format("Campaign with campaignId=%s and senderId=%s not found", campaignId, senderId)
                ));
    }

    private boolean isValid(Campaign campaign) {
        return !Objects.isNull(campaign)
                && StringUtils.hasText(campaign.getCampaignId())
                && StringUtils.hasText(campaign.getSenderId());
    }
}

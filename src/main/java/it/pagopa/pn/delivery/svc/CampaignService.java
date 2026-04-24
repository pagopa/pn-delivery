package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.MVPCampaignsParameterConsumer;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

    private final MVPCampaignsParameterConsumer mvpCampaignsParameterConsumer;

    public Campaign getCampaignByCampaignIdAndSenderId(String campaignId, String senderId ) {
        log.debug("Start getCampaignByCampaignIdAndSenderId for campaignId={} and senderId={}", campaignId, senderId);
        return mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(campaignId, senderId);
    }
}

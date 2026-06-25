package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.CampaignsParameterConsumer;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.rest.mapper.CampaignMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignsParameterConsumer campaignsParameterConsumer;

    public CampaignSearchResponse listCampaigns(String senderId, Integer size, String nextPagesKey) {
        List<Campaign> campaigns = campaignsParameterConsumer.getCampaignsBySenderId(senderId);

        List<CampaignSummary> allCampaigns = campaigns.stream()
                .map(CampaignMapper::toSummary)
                .toList();

        return new CampaignSearchResponse()
                .resultsPage(allCampaigns)
                .moreResult(false)
                .nextPagesKey(Collections.emptyList());
    }

    public CampaignDetail getCampaign(String campaignId, String senderId) {
        Campaign campaign = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(campaignId, senderId);
        return CampaignMapper.toDetail(campaign);
    }
}
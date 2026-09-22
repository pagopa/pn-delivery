package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.db.campaign.CampaignServiceCachedProvider;
import it.pagopa.pn.commons.db.campaign.entity.CampaignEntity;
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

    private final CampaignServiceCachedProvider campaignServiceProvider;

    public CampaignSearchResponse listCampaigns(String senderId, Integer size, String nextPagesKey) {
        List<Campaign> campaigns = campaignServiceProvider.getBySenderId(senderId).stream()
                .map(CampaignMapper::toInternalCampaign)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<CampaignSummary> allCampaigns = campaigns.stream()
                .map(CampaignMapper::toSummary)
                .toList();

        return new CampaignSearchResponse()
                .resultsPage(allCampaigns)
                .moreResult(false)
                .nextPagesKey(Collections.emptyList());
    }

    public CampaignDetail getCampaign(String campaignId, String senderId) {
        CampaignEntity campaignEntity = campaignServiceProvider.getByCampaignIdAndSenderId(campaignId, senderId);
        Campaign campaign = CampaignMapper.toInternalCampaign(campaignEntity);
        return CampaignMapper.toDetail(campaign);
    }
}
package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.commons.db.campaign.CampaignServiceCachedProvider;
import it.pagopa.pn.commons.db.campaign.entity.CampaignEntity;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.rest.mapper.CampaignMapper;
import it.pagopa.pn.delivery.utils.CampaignPaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignServiceCachedProvider campaignServiceProvider;

    /**
     * Ordinamento stabile e totale, necessario perche' lo slicing per pagina avvenga
     * su una sequenza deterministica anche tra refresh della cache.
     */
    private static final Comparator<CampaignSummary> CAMPAIGN_ORDER =
            Comparator.comparing(CampaignSummary::getStartDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(CampaignSummary::getCampaignId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    public CampaignSearchResponse listCampaigns(String senderId, Integer size, String nextPagesKey) {
        List<Campaign> campaigns = campaignServiceProvider.getBySenderId(senderId).stream()
                .map(CampaignMapper::toInternalCampaign)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<CampaignSummary> allCampaigns = campaigns.stream()
                .map(CampaignMapper::toSummary)
                .sorted(CAMPAIGN_ORDER)
                .toList();

        int pageSize = CampaignPaginationUtils.resolvePageSize(size);
        int offset = CampaignPaginationUtils.decodeOffset(nextPagesKey);
        int total = allCampaigns.size();

        if (offset > 0 && offset >= total) {
            throw CampaignPaginationUtils.invalidNextPagesKey();
        }

        int toIndex = Math.min(offset + pageSize, total);
        List<CampaignSummary> pageResults = offset >= total
                ? Collections.emptyList()
                : List.copyOf(allCampaigns.subList(offset, toIndex));
        boolean moreResult = toIndex < total;

        return new CampaignSearchResponse()
                .resultsPage(pageResults)
                .moreResult(moreResult)
                .nextPagesKey(moreResult
                        ? List.of(CampaignPaginationUtils.encodeOffset(toIndex))
                        : Collections.emptyList());
    }

    public CampaignDetail getCampaign(String campaignId, String senderId) {
        CampaignEntity campaignEntity = campaignServiceProvider.getByCampaignIdAndSenderId(campaignId, senderId);
        Campaign campaign = CampaignMapper.toInternalCampaign(campaignEntity);
        return CampaignMapper.toDetail(campaign);
    }
}
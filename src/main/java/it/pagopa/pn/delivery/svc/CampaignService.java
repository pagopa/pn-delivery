package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.CampaignsParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.rest.mapper.CampaignMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final CampaignsParameterConsumer campaignsParameterConsumer;

    public CampaignSearchResponse listCampaigns(String senderId, Integer size, String nextPagesKey) {
        List<Campaign> campaigns = campaignsParameterConsumer.getCampaignsBySenderId(senderId);

        int pageSize = normalizeSize(size);
        int startIndex = decodeStartIndex(nextPagesKey);
        if (startIndex >= campaigns.size()) {
            return new CampaignSearchResponse()
                    .resultsPage(Collections.emptyList())
                    .moreResult(false)
                    .nextPagesKey(Collections.emptyList());
        }

        int endIndex = Math.min(startIndex + pageSize, campaigns.size());
        List<CampaignSummary> currentPage = campaigns.subList(startIndex, endIndex)
                .stream()
                .map(CampaignMapper::toSummary)
                .toList();

        boolean hasMore = endIndex < campaigns.size();
        List<String> newNextPagesKey = hasMore
                ? List.of(encodeStartIndex(endIndex))
                : Collections.emptyList();

        return new CampaignSearchResponse()
                .resultsPage(currentPage)
                .moreResult(hasMore)
                .nextPagesKey(newNextPagesKey);
    }

    public CampaignDetail getCampaign(String campaignId, String senderId) {
        Campaign campaign = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(campaignId, senderId);
        return CampaignMapper.toDetail(campaign);
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int decodeStartIndex(String nextPagesKey) {
        if (nextPagesKey == null || nextPagesKey.isBlank()) {
            return 0;
        }

        try {
            String decoded = new String(Base64Utils.decodeFromUrlSafeString(nextPagesKey), StandardCharsets.UTF_8);
            return Integer.parseInt(decoded);
        } catch (IllegalArgumentException ex) {
            throw new PnBadRequestException(
                    "Invalid nextPagesKey",
                    "Unable to decode nextPagesKey",
                    ERROR_CODE_DELIVERY_UNSUPPORTED_LAST_EVALUATED_KEY,
                    ex
            );
        }
    }

    private String encodeStartIndex(int index) {
        return Base64Utils.encodeToUrlSafeString(String.valueOf(index).getBytes(StandardCharsets.UTF_8));
    }
}


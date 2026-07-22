package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.generated.openapi.server.v1.api.CampaignsApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.svc.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CampaignController implements CampaignsApi {

    private final CampaignService campaignService;

    @Override
    public ResponseEntity<CampaignSearchResponse> listCampaigns(
            UUID senderId,
            Integer size,
            String nextPagesKey) {
        log.debug("listCampaigns called with senderId={}, size={}", senderId, size);
        return ResponseEntity.ok(campaignService.listCampaigns(senderId.toString(), size, nextPagesKey));
    }

    @Override
    public ResponseEntity<CampaignDetail> getCampaign(
            String campaignId,
            UUID senderId) {
        log.debug("getCampaign called with campaignId={}, senderId={}", campaignId, senderId);
        return ResponseEntity.ok(campaignService.getCampaign(campaignId, senderId.toString()));
    }
}


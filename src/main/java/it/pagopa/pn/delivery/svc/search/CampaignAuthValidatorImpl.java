package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.svc.CampaignService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class CampaignAuthValidatorImpl implements CampaignAuthValidator {
    private final CampaignService campaignService;

    @Override
    public void checkCampaignIsFromSender(String campaignId, String senderId) {
        campaignService.getCampaign(campaignId, senderId);
    }
}

package it.pagopa.pn.delivery.svc.search;

public interface CampaignAuthValidator {
    void checkCampaignIsFromSender(String campaignId, String senderId);
}

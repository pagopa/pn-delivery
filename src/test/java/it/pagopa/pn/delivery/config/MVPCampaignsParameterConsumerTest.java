package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignException;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MVPCampaignsParameterConsumerTest {

    private static final String CAMPAIGN_ID = "campaign-id";
    private static final String SENDER_ID = "sender-id";

    @Mock
    private ParameterConsumer parameterConsumer;

    @InjectMocks
    private MVPCampaignsParameterConsumer mvpCampaignsParameterConsumer;

    @Test
    void getCampaignByCampaignIdAndSenderIdReturnsMatchingCampaign() {
        Campaign expectedCampaign = buildCampaign(CAMPAIGN_ID, SENDER_ID);
        Campaign otherCampaign = buildCampaign("other-campaign", "other-sender");

        when(parameterConsumer.getParameterValue("MVPCampaigns", Campaign[].class))
                .thenReturn(Optional.of(new Campaign[]{otherCampaign, expectedCampaign}));

        Campaign result = mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID);

        assertSame(expectedCampaign, result);
        verify(parameterConsumer).getParameterValue("MVPCampaigns", Campaign[].class);
    }

    @Test
    void getCampaignByCampaignIdAndSenderIdThrowsWhenParameterIsMissing() {
        when(parameterConsumer.getParameterValue("MVPCampaigns", Campaign[].class))
                .thenReturn(Optional.empty());

        PnCampaignException exception = assertThrows(PnCampaignException.class,
                () -> mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID));

        assertEquals("Campaign not found", exception.getMessage());
        verify(parameterConsumer).getParameterValue("MVPCampaigns", Campaign[].class);
    }

    @Test
    void getCampaignByCampaignIdAndSenderIdThrowsWhenNoCampaignMatches() {
        when(parameterConsumer.getParameterValue("MVPCampaigns", Campaign[].class))
                .thenReturn(Optional.of(new Campaign[]{buildCampaign("other-campaign", SENDER_ID)}));

        PnCampaignException exception = assertThrows(PnCampaignException.class,
                () -> mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID));

        assertEquals("Campaign not found", exception.getMessage());
        verify(parameterConsumer).getParameterValue("MVPCampaigns", Campaign[].class);
    }

    private Campaign buildCampaign(String campaignId, String senderId) {
        Campaign campaign = new Campaign();
        campaign.setCampaignId(campaignId);
        campaign.setSenderId(senderId);
        return campaign;
    }
}


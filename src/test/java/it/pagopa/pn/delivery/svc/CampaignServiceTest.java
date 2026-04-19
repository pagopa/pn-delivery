package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.MVPCampaignsParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.campaign.Campaign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    private static final String CAMPAIGN_ID = "campaign-id";
    private static final String SENDER_ID = "sender-id";

    @Mock
    private MVPCampaignsParameterConsumer mvpCampaignsParameterConsumer;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void getCampaignByCampaignIdAndSenderIdReturnsCampaignProvidedByConsumer() {
        Campaign expectedCampaign = new Campaign();
        when(mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID))
                .thenReturn(expectedCampaign);

        Campaign result = campaignService.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID);

        assertSame(expectedCampaign, result);
        verify(mvpCampaignsParameterConsumer).getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID);
    }

    @Test
    void getCampaignByCampaignIdAndSenderIdPropagatesConsumerException() {
        PnCampaignNotFoundException expectedException = new PnCampaignNotFoundException(
                "Campaign not found",
                "Campaign with campaignId=campaign-id and senderId=sender-id not found"
        );

        when(mvpCampaignsParameterConsumer.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID))
                .thenThrow(expectedException);

        PnCampaignNotFoundException exception = assertThrows(PnCampaignNotFoundException.class,
                () -> campaignService.getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID));

        assertSame(expectedException, exception);
        verify(mvpCampaignsParameterConsumer).getCampaignByCampaignIdAndSenderId(CAMPAIGN_ID, SENDER_ID);
    }
}


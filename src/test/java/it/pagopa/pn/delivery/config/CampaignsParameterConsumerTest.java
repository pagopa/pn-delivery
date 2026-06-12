package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.WorkflowStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

class CampaignsParameterConsumerTest {

    private ParameterConsumer parameterConsumer;
    private CampaignsParameterConsumer campaignsParameterConsumer;

    @BeforeEach
    void setup() {
        parameterConsumer = Mockito.mock(ParameterConsumer.class);
        campaignsParameterConsumer = new CampaignsParameterConsumer(parameterConsumer);
    }

    @Test
    void getCampaignsBySenderId_filtersCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-a").build(),
                Campaign.builder().campaignId("c2").senderId("sender-b").build(),
                Campaign.builder().campaignId("c3").senderId("sender-a").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("c1", result.get(0).getCampaignId());
        Assertions.assertEquals("c3", result.get(1).getCampaignId());
    }

    @Test
    void getCampaignsBySenderId_noResults() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-b").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCampaignsBySenderId_parameterNotFound() {
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_success() {
        Campaign campaign = Campaign.builder()
                .campaignId("c1")
                .senderId("sender-a")
                .title("Campaign 1")
                .descriptionScope("Description")
                .closed(false)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30))
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        WorkflowStep.builder()
                                .channel("IO")
                                .desiredFeedback("READ")
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        Campaign[] campaigns = new Campaign[] {campaign};

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        Campaign result = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", "sender-a");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCampaignId());
        Assertions.assertEquals("sender-a", result.getSenderId());
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_notFoundByCampaignId() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-a").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("missing", "sender-a"));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_notFoundBySenderId() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-b").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", "sender-a"));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_parameterNotFound() {
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", "sender-a"));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_multipleCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-a").build(),
                Campaign.builder().campaignId("c2").senderId("sender-a").build(),
                Campaign.builder().campaignId("c3").senderId("sender-a").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        Campaign result = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c2", "sender-a");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c2", result.getCampaignId());
    }
}


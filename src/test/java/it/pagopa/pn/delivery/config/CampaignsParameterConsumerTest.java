package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.ChannelType;
import it.pagopa.pn.delivery.models.internal.campaign.DesiredFeedbackType;
import it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

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
        campaignsParameterConsumer.initialize();

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
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCampaignsBySenderId_parameterNotFound() {
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void initialize_parameterNotFoundExceptionDoesNotBreakStartup() {
        PnInternalException exception = new PnInternalException(
                "Internal Server Error",
                "GENERIC_ERROR",
                ParameterNotFoundException.builder().message("Parameter MVPCampaigns not found.").build()
        );

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenThrow(exception);

        Assertions.assertDoesNotThrow(() -> campaignsParameterConsumer.initialize());
        Assertions.assertTrue(campaignsParameterConsumer.getCampaignsBySenderId("sender-a").isEmpty());
    }

    @Test
    void initialize_unexpectedInternalExceptionIsPropagated() {
        PnInternalException exception = new PnInternalException("boom", "GENERIC_ERROR");

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenThrow(exception);

        Assertions.assertThrows(PnInternalException.class, () -> campaignsParameterConsumer.initialize());
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_success() {
        Campaign campaign = Campaign.builder()
                .campaignId("c1")
                .senderId("sender-a")
                .title("Campaign 1")
                .descriptionScope("Description")
                .closed(false)
                .startDate(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .endDate(OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC))
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.valueOf("IO"))
                                .recipientType(RecipientTypeInt.PF)
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(DesiredFeedbackType.valueOf("READ"))
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(new Campaign[] {campaign}));
        campaignsParameterConsumer.initialize();

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
        campaignsParameterConsumer.initialize();

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
        campaignsParameterConsumer.initialize();

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", "sender-a"));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_parameterNotFound() {
        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());
        campaignsParameterConsumer.initialize();

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
        campaignsParameterConsumer.initialize();

        Campaign result = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c2", "sender-a");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c2", result.getCampaignId());
    }

    @Test
    void parameterStoreIsReadOnlyAtInitialization() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-a").build()
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        campaignsParameterConsumer.initialize();

        campaignsParameterConsumer.getCampaignsBySenderId("sender-a");
        campaignsParameterConsumer.getCampaignsBySenderId("sender-b");
        campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", "sender-a");

        Mockito.verify(parameterConsumer, Mockito.times(1))
                .getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class));
    }

    @Test
    void initialize_skipsInvalidCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                Campaign.builder().campaignId("c1").senderId("sender-a").build(),
                Campaign.builder().campaignId(null).senderId("sender-a").build(),
                Campaign.builder().campaignId("c3").senderId(null).build(),
                null
        };

        Mockito.when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId("sender-a");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("c1", result.get(0).getCampaignId());
    }
}

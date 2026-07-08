package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import static org.mockito.Mockito.*;

class CampaignsParameterConsumerTest {

    private static final String SENDER_A = "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce";
    private static final String SENDER_B = "138f5f86-954b-4c65-a556-85b7f5f3958a";

    private ParameterConsumer parameterConsumer;
    private CampaignsParameterConsumer campaignsParameterConsumer;

    @BeforeEach
    void setup() {
        parameterConsumer = mock(ParameterConsumer.class);
        campaignsParameterConsumer = new CampaignsParameterConsumer(parameterConsumer);
    }

    @Test
    void getCampaignsBySenderId_filtersCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_A),
                validCampaign("c2", SENDER_B),
                validCampaign("c3", SENDER_A)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("c1", result.get(0).getCampaignId());
        Assertions.assertEquals("c3", result.get(1).getCampaignId());
    }

    @Test
    void getCampaignsBySenderId_noResults() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_B)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCampaignsBySenderId_parameterNotFound() {
        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void initialize_parameterNotFoundExceptionDoesNotBreakStartup() {
        PnInternalException exception = new PnInternalException(
                "Internal Server Error",
                "GENERIC_ERROR",
                ParameterNotFoundException.builder().message("Parameter MVPCampaigns not found.").build()
        );

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenThrow(exception);

        Assertions.assertDoesNotThrow(() -> campaignsParameterConsumer.initialize());
        Assertions.assertTrue(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A).isEmpty());
    }

    @Test
    void initialize_unexpectedInternalExceptionIsPropagated() {
        PnInternalException exception = new PnInternalException("boom", "GENERIC_ERROR");

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenThrow(exception);

        Assertions.assertThrows(PnInternalException.class, () -> campaignsParameterConsumer.initialize());
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_success() {
        Campaign campaign = validCampaign("c1", SENDER_A);

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(new Campaign[] {campaign}));
        campaignsParameterConsumer.initialize();

        Campaign result = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_A);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCampaignId());
        Assertions.assertEquals(SENDER_A, result.getSenderId());
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_notFoundByCampaignId() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_A)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("missing", SENDER_A));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_notFoundBySenderId() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_B)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_A));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_parameterNotFound() {
        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.empty());
        campaignsParameterConsumer.initialize();

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_A));
    }

    @Test
    void getCampaignByCampaignIdAndSenderId_multipleCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_A),
                validCampaign("c2", SENDER_A),
                validCampaign("c3", SENDER_A)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        Campaign result = campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c2", SENDER_A);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c2", result.getCampaignId());
    }

    @Test
    void parameterStoreIsReadOnlyAtInitialization() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_A)
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));

        campaignsParameterConsumer.initialize();

        campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A);
        campaignsParameterConsumer.getCampaignsBySenderId(SENDER_B);
        campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_A);

        verify(parameterConsumer, times(1))
                .getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class));
    }

    @Test
    void initialize_skipsInvalidCampaigns() {
        Campaign[] campaigns = new Campaign[] {
                validCampaign("c1", SENDER_A),
                validCampaign("c2", "not-a-uuid"),
                validCampaign("c3", SENDER_A).toBuilder().channels(List.of()).build(),
                validCampaign("c4", SENDER_A).toBuilder().workflow(List.of(validWorkflowStep(), WorkFlowEntity.builder().build())).build(),
                null
        };

        when(parameterConsumer.getParameterValue(Mockito.anyString(), Mockito.eq(Campaign[].class)))
                .thenReturn(Optional.of(campaigns));
        campaignsParameterConsumer.initialize();

        List<Campaign> result = campaignsParameterConsumer.getCampaignsBySenderId(SENDER_A);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("c1", result.get(0).getCampaignId());
    }

    private Campaign validCampaign(String campaignId, String senderId) {
        return Campaign.builder()
                .campaignId(campaignId)
                .senderId(senderId)
                .title("Campaign " + campaignId)
                .descriptionScope("Description " + campaignId)
                .status(CampaignStatus.IN_PROGRESS)
                .startDate(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .endDate(OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC))
                .serviceId("service-" + campaignId)
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO, ChannelType.SMS))
                .workflow(List.of(validWorkflowStep()))
                .build();
    }

    private WorkFlowEntity validWorkflowStep() {
        return WorkFlowEntity.builder()
                .channel(ChannelType.IO)
                .recipientType(Collections.singleton(RecipientTypeInt.PF))
                .timeout(Duration.ofDays(1))
                .desiredFeedback(DesiredFeedbackType.READ)
                .includeAttachment(false)
                .build();
    }
}

package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.CampaignsParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.ChannelType;
import it.pagopa.pn.delivery.models.internal.campaign.DesiredFeedbackType;
import it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity;
import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

class CampaignServiceTest {

    private static final String SENDER_ID = "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce";

    private CampaignsParameterConsumer campaignsParameterConsumer;
    private CampaignService campaignService;

    @BeforeEach
    void setup() {
        campaignsParameterConsumer = Mockito.mock(CampaignsParameterConsumer.class);
        campaignService = new CampaignService(campaignsParameterConsumer);
    }

    @Test
    void listCampaigns_firstPage() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2"),
                        validCampaign("c3")
                ));

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 2, null);

        Assertions.assertEquals(2, response.getResultsPage().size());
        Assertions.assertTrue(response.getMoreResult());
        Assertions.assertEquals(1, response.getNextPagesKey().size());
    }

    @Test
    void listCampaigns_secondPage() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2"),
                        validCampaign("c3")
                ));

        String nextPagesKey = Base64Utils.encodeToUrlSafeString("2".getBytes(StandardCharsets.UTF_8));

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 2, nextPagesKey);

        Assertions.assertEquals(1, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
        Assertions.assertEquals("c3", response.getResultsPage().get(0).getCampaignId());
    }

    @Test
    void listCampaigns_singlePage_noMoreResults() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2")
                ));

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 10, null);

        Assertions.assertEquals(2, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_emptyList() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(Collections.emptyList());

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 10, null);

        Assertions.assertTrue(response.getResultsPage().isEmpty());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_nullSize_usesDefault() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2"),
                        validCampaign("c3"),
                        validCampaign("c4")
                ));

        // With null size, should default to 10
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, null, null);

        Assertions.assertEquals(4, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
    }

    @Test
    void listCampaigns_outOfBoundsNextPageKey() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2")
                ));

        String nextPagesKey = Base64Utils.encodeToUrlSafeString("10".getBytes(StandardCharsets.UTF_8));

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 10, nextPagesKey);

        Assertions.assertTrue(response.getResultsPage().isEmpty());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_maxPageSizeExceeded() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2")
                ));

        // Size is capped at 50 max
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 100, null);

        Assertions.assertEquals(2, response.getResultsPage().size());
    }

    @Test
    void listCampaigns_invalidNextPageKey() {
        Mockito.when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(validCampaign("c1")));

        Assertions.assertThrows(PnBadRequestException.class,
                () -> campaignService.listCampaigns(SENDER_ID, 10, "not-valid-base64"));
    }

    @Test
    void getCampaign_success() {
        OffsetDateTime now = OffsetDateTime.now();
        Campaign campaign = Campaign.builder()
                .campaignId("c1")
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .descriptionScope("Description")
                .closed(false)
                .startDate(now)
                .endDate(now.plusDays(30))
                .senderContact("contact@example.com")
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO, ChannelType.SMS))
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(RecipientTypeInt.PF)
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        Mockito.when(campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_ID))
                .thenReturn(campaign);

        CampaignDetail result = campaignService.getCampaign("c1", SENDER_ID);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCampaignId());
        Assertions.assertEquals("Campaign 1", result.getTitle());
        Assertions.assertEquals("Description", result.getDescriptionScope());
        Assertions.assertFalse(result.getClosed());
        Assertions.assertEquals("contact@example.com", result.getSenderContact());
        Assertions.assertEquals(2, result.getChannels().size());
        Assertions.assertEquals(1, result.getWorkflow().size());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"), result.getWorkflow().get(0).getChannel());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue("PF"), result.getWorkflow().get(0).getRecipientType());
        Assertions.assertEquals("PT24H", result.getWorkflow().get(0).getTimeout());
    }

    @Test
    void getCampaign_notFound() {
        Mockito.when(campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("missing", SENDER_ID))
                .thenThrow(new PnCampaignNotFoundException("Campaign not found"));

        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignService.getCampaign("missing", SENDER_ID));
    }

    private Campaign validCampaign(String campaignId) {
        OffsetDateTime now = OffsetDateTime.now();
        return Campaign.builder()
                .campaignId(campaignId)
                .senderId(SENDER_ID)
                .title("title-" + campaignId)
                .descriptionScope("description-" + campaignId)
                .closed(false)
                .startDate(now)
                .endDate(now.plusDays(30))
                .serviceId("service-" + campaignId)
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO, ChannelType.SMS))
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(RecipientTypeInt.PF)
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build()
                ))
                .build();
    }
}


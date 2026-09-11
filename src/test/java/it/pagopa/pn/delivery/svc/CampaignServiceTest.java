package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.db.campaign.CampaignServiceCachedProvider;
import it.pagopa.pn.commons.db.campaign.entity.*;
import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

class CampaignServiceTest {

    private static final String SENDER_ID = "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce";

    private CampaignServiceCachedProvider campaignServiceProvider;
    private CampaignService campaignService;

    @BeforeEach
    void setup() {
        campaignServiceProvider = Mockito.mock(CampaignServiceCachedProvider.class);
        campaignService = new CampaignService(campaignServiceProvider);
    }

    @Test
    void listCampaigns_returnsAllCampaigns() {
        // Arrange
        when(campaignServiceProvider.getBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaignEntity("c1"),
                        validCampaignEntity("c2"),
                        validCampaignEntity("c3")
                ));

        // Act
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, null, null);

        // Assert
        Assertions.assertEquals(3, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_emptyList() {
        // Arrange
        when(campaignServiceProvider.getBySenderId(SENDER_ID))
                .thenReturn(Collections.emptyList());

        // Act
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 10, null);

        // Assert
        Assertions.assertTrue(response.getResultsPage().isEmpty());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_ignoresPaginationParameters() {
        // Arrange
        when(campaignServiceProvider.getBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaignEntity("c1"),
                        validCampaignEntity("c2")
                ));

        // Act
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 1, "any-string-not-base64");

        // Assert
        Assertions.assertEquals(2, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void listCampaigns_filtersNullCampaignEntities() {
        when(campaignServiceProvider.getBySenderId(SENDER_ID))
                .thenReturn(Arrays.asList(validCampaignEntity("c1"), null, validCampaignEntity("c2")));

        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, null, null);

        Assertions.assertEquals(2, response.getResultsPage().size());
    }

    @Test
    void getCampaign_success() {
        // Arrange
        CampaignEntity campaignEntity = validCampaignEntity("c1");

        when(campaignServiceProvider.getByCampaignIdAndSenderId("c1", SENDER_ID))
                .thenReturn(campaignEntity);

        // Act
        CampaignDetail result = campaignService.getCampaign("c1", SENDER_ID);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCampaignId());
        Assertions.assertEquals("title-c1", result.getTitle());
        Assertions.assertEquals("description-c1", result.getDescriptionScope());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignStatus.IN_PROGRESS, result.getCampaignStatus());
        Assertions.assertEquals("contact@example.com", result.getSenderContact());
        Assertions.assertEquals(1, result.getWorkflow().size());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"), result.getWorkflow().get(0).getChannel());

        Set<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt> recipients = result.getWorkflow().get(0).getRecipientType();
        Assertions.assertEquals(1, recipients.size());
        Assertions.assertTrue(recipients.contains(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue("PF")));
    }

    @Test
    void getCampaign_notFound() {
        // Arrange
        when(campaignServiceProvider.getByCampaignIdAndSenderId("missing", SENDER_ID))
                .thenThrow(new PnCampaignNotFoundException("Campaign not found"));

        // Act & Assert
        Assertions.assertThrows(PnCampaignNotFoundException.class,
                () -> campaignService.getCampaign("missing", SENDER_ID));
    }

    private CampaignEntity validCampaignEntity(String campaignId) {
        OffsetDateTime now = OffsetDateTime.now();
        return CampaignEntity.builder()
                .campaignId(campaignId)
                .senderId(SENDER_ID)
                .title("title-" + campaignId)
                .descriptionScope("description-" + campaignId)
                .status(CampaignStatus.IN_PROGRESS)
                .startDate(now.toInstant())
                .endDate(now.plusDays(30).toInstant())
                .senderContact("contact@example.com")
                .serviceId("service-" + campaignId)
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        WorkflowEntity.builder()
                                .channel(CampaignChannel.IO)
                                .recipientType(Set.of(RecipientTypeInt.PF))
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(Set.of(DesiredFeedback.READ))
                                .includeAttachment(false)
                                .build()
                ))
                .build();
    }
}
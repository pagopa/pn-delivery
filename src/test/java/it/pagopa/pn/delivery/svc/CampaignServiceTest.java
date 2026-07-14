package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.config.CampaignsParameterConsumer;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSearchResponse;
import it.pagopa.pn.delivery.models.internal.campaign.*;
import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;

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
    void listCampaigns_returnsAllCampaigns() {
        // Arrange
        when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2"),
                        validCampaign("c3")
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
        when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
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
        when(campaignsParameterConsumer.getCampaignsBySenderId(SENDER_ID))
                .thenReturn(List.of(
                        validCampaign("c1"),
                        validCampaign("c2")
                ));

        // Act: Passiamo parametri di paginazione stringenti e una chiave teoricamente invalida
        // Il servizio deve ignorarli completamente e restituire tutto senza lanciare eccezioni
        CampaignSearchResponse response = campaignService.listCampaigns(SENDER_ID, 1, "any-string-not-base64");

        // Assert
        Assertions.assertEquals(2, response.getResultsPage().size());
        Assertions.assertFalse(response.getMoreResult());
        Assertions.assertTrue(response.getNextPagesKey().isEmpty());
    }

    @Test
    void getCampaign_success() {
        // Arrange
        OffsetDateTime now = OffsetDateTime.now();
        Campaign campaign = Campaign.builder()
                .campaignId("c1")
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .descriptionScope("Description")
                .status(CampaignStatus.IN_PROGRESS)
                .startDate(now)
                .endDate(now.plusDays(30))
                .senderContact("contact@example.com")
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Set.of(RecipientTypeInt.PF))
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(Set.of(DesiredFeedbackType.READ))
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        when(campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("c1", SENDER_ID))
                .thenReturn(campaign);

        // Act
        CampaignDetail result = campaignService.getCampaign("c1", SENDER_ID);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals("c1", result.getCampaignId());
        Assertions.assertEquals("Campaign 1", result.getTitle());
        Assertions.assertEquals("Description", result.getDescriptionScope());
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
        when(campaignsParameterConsumer.getCampaignByCampaignIdAndSenderId("missing", SENDER_ID))
                .thenThrow(new PnCampaignNotFoundException("Campaign not found"));

        // Act & Assert
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
                .status(CampaignStatus.IN_PROGRESS)
                .startDate(now)
                .endDate(now.plusDays(30))
                .serviceId("service-" + campaignId)
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Set.of(RecipientTypeInt.PF))
                                .timeout(Duration.ofDays(1))
                                .desiredFeedback(Set.of(DesiredFeedbackType.READ))
                                .includeAttachment(false)
                                .build()
                ))
                .build();
    }
}
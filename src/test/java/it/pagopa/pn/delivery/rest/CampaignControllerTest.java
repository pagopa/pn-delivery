package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.svc.CampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WebFluxTest(controllers = CampaignController.class)
class CampaignControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    CampaignService campaignService;

    private static final String CAMPAIGN_ID = "c1";
    private static final UUID SENDER_ID = UUID.fromString("5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce");
    private static final String BASE_PATH = "/delivery-private/v1/campaigns";

    @Test
    void listCampaigns_success() {
        // Given
        CampaignSummary summary = new CampaignSummary()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .pfChannels(List.of(ChannelType.IO))
                .pgChannels(List.of(ChannelType.PEC))
                .campaignStatus(CampaignStatus.IN_PROGRESS)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30));

        CampaignSearchResponse response = new CampaignSearchResponse()
                .resultsPage(List.of(summary))
                .moreResult(false)
                .nextPagesKey(Collections.emptyList());

        when(campaignService.listCampaigns(eq(SENDER_ID.toString()), eq(10), isNull()))
                .thenReturn(response);

        // When & Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("senderId", SENDER_ID.toString())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignSearchResponse.class)
                .value(result -> {
                    assert result.getResultsPage().size() == 1;
                    assert result.getResultsPage().get(0).getCampaignId().equals(CAMPAIGN_ID);
                    assert !result.getMoreResult();
                });
    }

    @Test
    void listCampaigns_withPagination() {
        // Given
        CampaignSummary summary1 = new CampaignSummary()
                .campaignId("c1")
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .pfChannels(List.of(ChannelType.IO))
                .pgChannels(List.of(ChannelType.PEC))
                .campaignStatus(CampaignStatus.IN_PROGRESS)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30));

        CampaignSummary summary2 = new CampaignSummary()
                .campaignId("c2")
                .senderId(SENDER_ID)
                .title("Campaign 2")
                .pfChannels(List.of(ChannelType.SMS))
                .pgChannels(List.of(ChannelType.ANALOG))
                .campaignStatus(CampaignStatus.IN_PROGRESS)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30));

        String nextPageKey = "TQ==";

        CampaignSearchResponse response = new CampaignSearchResponse()
                .resultsPage(List.of(summary1, summary2))
                .moreResult(true)
                .nextPagesKey(List.of(nextPageKey));

        when(campaignService.listCampaigns(eq(SENDER_ID.toString()), eq(20), isNull()))
                .thenReturn(response);

        // When & Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("senderId", SENDER_ID.toString())
                        .queryParam("size", 20)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignSearchResponse.class)
                .value(result -> {
                    assert result.getResultsPage().size() == 2;
                    assert result.getMoreResult();
                    assert result.getNextPagesKey().size() == 1;
                });
    }

    @Test
    void listCampaigns_empty() {
        // Given
        CampaignSearchResponse response = new CampaignSearchResponse()
                .resultsPage(Collections.emptyList())
                .moreResult(false)
                .nextPagesKey(Collections.emptyList());

        when(campaignService.listCampaigns(eq(SENDER_ID.toString()), eq(10), isNull()))
                .thenReturn(response);

        // When & Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_PATH)
                        .queryParam("senderId", SENDER_ID.toString())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignSearchResponse.class)
                .value(result -> {
                    assert result.getResultsPage().isEmpty();
                    assert !result.getMoreResult();
                });
    }

    @Test
    void getCampaign_success() {
        // Given
        CampaignDetail detail = new CampaignDetail()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .descriptionScope("Test description")
                .campaignStatus(CampaignStatus.IN_PROGRESS)
                .senderContact("contact@example.com")
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30))
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .workflow(List.of(
                        new WorkflowEntity()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PF))
                                .timeout("PT24H")
                                .desiredFeedback(Set.of(DesiredFeedbackType.READ))
                                .includeAttachment(false)
                ));

        when(campaignService.getCampaign(CAMPAIGN_ID, SENDER_ID.toString()))
                .thenReturn(detail);

        // When & Then
        webTestClient.get()
                .uri(BASE_PATH + "/" + CAMPAIGN_ID + "?senderId=" + SENDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignDetail.class)
                .value(result -> {
                    assert result.getCampaignId().equals(CAMPAIGN_ID);
                    assert result.getTitle().equals("Campaign 1");
                    assert result.getDescriptionScope().equals("Test description");
                    assert result.getCampaignStatus().equals(CampaignStatus.IN_PROGRESS);
                });
    }

    @Test
    void getCampaign_notFound() {
        // Given
        when(campaignService.getCampaign("missing", SENDER_ID.toString()))
                .thenThrow(new PnCampaignNotFoundException("Campaign not found"));

        // When & Then
        webTestClient.get()
                .uri(BASE_PATH + "/missing?senderId=" + SENDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getCampaign_withWorkflow() {
        // Given
        CampaignDetail detail = new CampaignDetail()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title("Campaign 1")
                .descriptionScope("Test description")
                .campaignStatus(CampaignStatus.IN_PROGRESS)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30))
                .serviceId("service-1")
                .sensitiveContent(true)
                .stopOnViewed(true)
                .workflow(List.of(
                        new WorkflowEntity()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PF))
                                .timeout("PT24H")
                                .desiredFeedback(Set.of(DesiredFeedbackType.READ))
                                .includeAttachment(false),
                        new WorkflowEntity()
                                .channel(ChannelType.PEC)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout("PT48H")
                                .desiredFeedback(Set.of(DesiredFeedbackType.RECEIVED))
                                .includeAttachment(true)
                ));

        when(campaignService.getCampaign(CAMPAIGN_ID, SENDER_ID.toString()))
                .thenReturn(detail);

        // When & Then
        webTestClient.get()
                .uri(BASE_PATH + "/" + CAMPAIGN_ID + "?senderId=" + SENDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignDetail.class)
                .value(result -> {
                    assert result.getWorkflow().size() == 2;
                    assert result.getSensitiveContent();
                    assert result.getStopOnViewed();
                });
    }

    @Test
    void listCampaigns_missingRequiredParam() {
        // When & Then - Missing senderId should result in a bad request
        webTestClient.get()
                .uri(BASE_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getCampaign_missingQueryParam() {
        // When & Then - Missing senderId query param should result in bad request
        webTestClient.get()
                .uri(BASE_PATH + "/" + CAMPAIGN_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void campaignsBasePathWithTrailingSlash_routesToListCampaigns() {
        // Given
        CampaignSearchResponse response = new CampaignSearchResponse()
                .resultsPage(Collections.emptyList())
                .moreResult(false)
                .nextPagesKey(Collections.emptyList());

        when(campaignService.listCampaigns(eq(SENDER_ID.toString()), eq(10), isNull()))
                .thenReturn(response);

        // When & Then - /campaigns/?senderId=... targets listCampaigns endpoint
        webTestClient.get()
                .uri(BASE_PATH + "/?senderId=" + SENDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CampaignSearchResponse.class)
                .value(result -> {
                    assert result.getResultsPage().isEmpty();
                    assert !result.getMoreResult();
                });

        verify(campaignService).listCampaigns(eq(SENDER_ID.toString()), eq(10), isNull());
        verify(campaignService, never()).getCampaign(anyString(), anyString());
    }
}


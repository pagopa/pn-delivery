package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.ChannelType;
import it.pagopa.pn.delivery.models.internal.campaign.DesiredFeedbackType;
import it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity;
import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class CampaignMapperTest {

    private static final String CAMPAIGN_ID = "c1";
    private static final String SENDER_ID = "5b994d4a-0fa8-47ac-9c7b-354f1d44a1ce";
    private static final String TITLE = "Campaign Title";
    private static final String DESCRIPTION = "Campaign Description";

    @Test
    void toSummary_success() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.plusDays(30);

        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(24))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.SMS)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(48))
                                .desiredFeedback(DesiredFeedbackType.RECEIVED)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.PEC)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(72))
                                .desiredFeedback(DesiredFeedbackType.RECEIVED)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.PEC)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(24))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(48))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.ANALOG)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(96))
                                .desiredFeedback(DesiredFeedbackType.SKIP)
                                .includeAttachment(true)
                                .build()
                ))
                .closed(false)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // When
        CampaignSummary summary = CampaignMapper.toSummary(campaign);

        // Then
        Assertions.assertNotNull(summary);
        Assertions.assertEquals(CAMPAIGN_ID, summary.getCampaignId());
        Assertions.assertEquals(UUID.fromString(SENDER_ID), summary.getSenderId());
        Assertions.assertEquals(TITLE, summary.getTitle());
        Assertions.assertEquals(List.of(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"),
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("SMS")
        ), summary.getPfChannels());
        Assertions.assertEquals(List.of(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("PEC"),
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("ANALOG")
        ), summary.getPgChannels());
        Assertions.assertFalse(summary.getClosed());
        Assertions.assertEquals(startDate, summary.getStartDate());
        Assertions.assertEquals(endDate, summary.getEndDate());
    }

    @Test
    void toSummary_campaignClosed() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .workflow(List.of())
                .closed(true)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .build();

        // When
        CampaignSummary summary = CampaignMapper.toSummary(campaign);

        // Then
        Assertions.assertTrue(summary.getClosed());
        Assertions.assertTrue(summary.getPfChannels().isEmpty());
        Assertions.assertTrue(summary.getPgChannels().isEmpty());
    }

    @Test
    void toDetail_success() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.plusDays(30);

        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(false)
                .senderContact("contact@example.com")
                .startDate(startDate)
                .endDate(endDate)
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO, ChannelType.SMS))
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(24))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertNotNull(detail);
        Assertions.assertEquals(CAMPAIGN_ID, detail.getCampaignId());
        Assertions.assertEquals(UUID.fromString(SENDER_ID), detail.getSenderId());
        Assertions.assertEquals(TITLE, detail.getTitle());
        Assertions.assertEquals(DESCRIPTION, detail.getDescriptionScope());
        Assertions.assertFalse(detail.getClosed());
        Assertions.assertEquals("contact@example.com", detail.getSenderContact());
        Assertions.assertEquals(startDate, detail.getStartDate());
        Assertions.assertEquals(endDate, detail.getEndDate());
        Assertions.assertEquals("service-1", detail.getServiceId());
        Assertions.assertFalse(detail.getSensitiveContent());
        Assertions.assertFalse(detail.getStopOnViewed());
        Assertions.assertEquals(List.of(
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"),
                it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("SMS")
        ), detail.getChannels());

        // Check workflow
        Assertions.assertNotNull(detail.getWorkflow());
        Assertions.assertEquals(1, detail.getWorkflow().size());
        WorkflowEntity workflow = detail.getWorkflow().get(0);
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"), workflow.getChannel());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue("PF"), workflow.getRecipientType());
        Assertions.assertEquals("PT24H", workflow.getTimeout());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue("READ"), workflow.getDesiredFeedback());
        Assertions.assertFalse(workflow.getIncludeAttachment());
    }

    @Test
    void toDetail_nullWorkflow() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(false)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO))
                .workflow(null)
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertNotNull(detail.getWorkflow());
        Assertions.assertTrue(detail.getWorkflow().isEmpty());
    }

    @Test
    void toDetail_emptyWorkflow() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(false)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO))
                .workflow(List.of())
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertNotNull(detail.getWorkflow());
        Assertions.assertTrue(detail.getWorkflow().isEmpty());
    }

    @Test
    void toDetail_multipleWorkflowSteps() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(false)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO, ChannelType.PEC, ChannelType.ANALOG))
                .workflow(List.of(
                        WorkFlowEntity.builder()
                                .channel(ChannelType.IO)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(24))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.PEC)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(48))
                                .desiredFeedback(DesiredFeedbackType.RECEIVED)
                                .includeAttachment(true)
                                .build(),
                        WorkFlowEntity.builder()
                                .channel(ChannelType.ANALOG)
                                .recipientType(Collections.singleton(RecipientTypeInt.PG))
                                .timeout(Duration.ofHours(72))
                                .desiredFeedback(DesiredFeedbackType.READ)
                                .includeAttachment(false)
                                .build()
                ))
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertEquals(3, detail.getWorkflow().size());

        WorkflowEntity workflow1 = detail.getWorkflow().get(0);
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("IO"), workflow1.getChannel());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue("READ"), workflow1.getDesiredFeedback());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue("PF"), workflow1.getRecipientType());
        Assertions.assertEquals("PT24H", workflow1.getTimeout());

        WorkflowEntity workflow2 = detail.getWorkflow().get(1);
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("PEC"), workflow2.getChannel());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue("RECEIVED"), workflow2.getDesiredFeedback());
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue("PG"), workflow2.getRecipientType());
        Assertions.assertEquals("PT48H", workflow2.getTimeout());
        Assertions.assertTrue(workflow2.getIncludeAttachment());

        WorkflowEntity workflow3 = detail.getWorkflow().get(2);
        Assertions.assertEquals(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue("ANALOG"), workflow3.getChannel());
        Assertions.assertEquals("PT72H", workflow3.getTimeout());
    }

    @Test
    void toDetail_campaignWithSensitiveContent() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(false)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .serviceId("service-1")
                .sensitiveContent(true)
                .stopOnViewed(true)
                .channels(List.of(ChannelType.IO))
                .workflow(List.of())
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertTrue(detail.getSensitiveContent());
        Assertions.assertTrue(detail.getStopOnViewed());
    }

    @Test
    void toDetail_campaignClosed() {
        // Given
        Campaign campaign = Campaign.builder()
                .campaignId(CAMPAIGN_ID)
                .senderId(SENDER_ID)
                .title(TITLE)
                .descriptionScope(DESCRIPTION)
                .closed(true)
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now())
                .serviceId("service-1")
                .sensitiveContent(false)
                .stopOnViewed(false)
                .channels(List.of(ChannelType.IO))
                .workflow(List.of())
                .build();

        // When
        CampaignDetail detail = CampaignMapper.toDetail(campaign);

        // Then
        Assertions.assertTrue(detail.getClosed());
    }
}


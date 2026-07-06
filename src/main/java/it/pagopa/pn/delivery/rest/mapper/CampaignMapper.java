package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity;

import java.util.*;
import java.util.stream.Collectors;

public final class CampaignMapper {

    private CampaignMapper() {
    }

    public static CampaignSummary toSummary(Campaign campaign) {
        return new CampaignSummary()
                .campaignId(campaign.getCampaignId())
                .senderId(UUID.fromString(campaign.getSenderId()))
                .title(campaign.getTitle())
                .pfChannels(extractSummaryChannels(campaign.getWorkflow(), RecipientTypeInt.PF))
                .pgChannels(extractSummaryChannels(campaign.getWorkflow(), RecipientTypeInt.PG))
                .closed(Boolean.TRUE.equals(campaign.getClosed()))
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate());
    }

    public static CampaignDetail toDetail(Campaign campaign) {
        CampaignDetail detail = new CampaignDetail()
                .campaignId(campaign.getCampaignId())
                .senderId(UUID.fromString(campaign.getSenderId()))
                .title(campaign.getTitle())
                .descriptionScope(campaign.getDescriptionScope())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .closed(Boolean.TRUE.equals(campaign.getClosed()))
                .senderContact(campaign.getSenderContact())
                .serviceId(campaign.getServiceId())
                .sensitiveContent(Boolean.TRUE.equals(campaign.getSensitiveContent()))
                .channels(mapChannels(campaign.getChannels()))
                .stopOnViewed(Boolean.TRUE.equals(campaign.getStopOnViewed()));

        List<WorkflowEntity> workflow = campaign.getWorkflow() == null
                ? Collections.emptyList()
                : campaign.getWorkflow().stream()
                .filter(Objects::nonNull)
                .map(CampaignMapper::toWorkflowEntity)
                .toList();

        detail.workflow(workflow);
        return detail;
    }

    private static List<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType> mapChannels(
            List<it.pagopa.pn.delivery.models.internal.campaign.ChannelType> channels) {
        if (channels == null) {
            return Collections.emptyList();
        }

        return channels.stream()
                .filter(Objects::nonNull)
                .map(channel -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue(channel.name()))
                .toList();
    }

    private static List<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType> extractSummaryChannels(
            List<WorkFlowEntity> workflow,
            RecipientTypeInt recipientType) {
        if (workflow == null) {
            return Collections.emptyList();
        }

        return workflow.stream()
                .filter(Objects::nonNull)
                .filter(step -> step.getRecipientType() != null && step.getRecipientType().contains(recipientType))
                .map(WorkFlowEntity::getChannel)
                .filter(Objects::nonNull)
                .distinct()
                .map(channel -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue(channel.name()))
                .toList();
    }

    private static WorkflowEntity toWorkflowEntity(WorkFlowEntity step) {
        WorkflowEntity workflowEntity = new WorkflowEntity()
                .includeAttachment(step.getIncludeAttachment());

        if (step.getChannel() != null) {
            workflowEntity.channel(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue(step.getChannel().name()));
        }

        if (step.getRecipientType() != null) {
            Set<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt> mappedRecipientTypes = step.getRecipientType().stream()
                    .filter(Objects::nonNull)
                    .map(type -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue(type.name()))
                    .collect(Collectors.toSet());

            workflowEntity.recipientType(mappedRecipientTypes);
        }

        if (step.getTimeout() != null) {
            workflowEntity.timeout(step.getTimeout().toString());
        }

        if (step.getDesiredFeedback() != null) {
            workflowEntity.desiredFeedback(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue(step.getDesiredFeedback().name()));
        }

        return workflowEntity;
    }
}


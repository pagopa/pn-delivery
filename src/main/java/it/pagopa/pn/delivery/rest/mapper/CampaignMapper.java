package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignStatus;
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
                .campaignStatus(mapCampaignStatus(campaign.getStatus()))
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
                .campaignStatus(mapCampaignStatus(campaign.getStatus()))
                .senderContact(campaign.getSenderContact())
                .serviceId(campaign.getServiceId())
                .sensitiveContent(Boolean.TRUE.equals(campaign.getSensitiveContent()))
                .stopOnViewed(Boolean.TRUE.equals(campaign.getStopOnViewed()))
                .taxonomyCode(campaign.getTaxonomyCode());

        List<WorkflowEntity> workflow = campaign.getWorkflow() == null
                ? Collections.emptyList()
                : campaign.getWorkflow().stream()
                .filter(Objects::nonNull)
                .map(CampaignMapper::toWorkflowEntity)
                .toList();

        detail.workflow(workflow);
        return detail;
    }

    private static CampaignStatus mapCampaignStatus(
            it.pagopa.pn.delivery.models.internal.campaign.CampaignStatus status) {
        if (status == null) {
            return null;
        }
        return CampaignStatus.fromValue(status.name());
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
            workflowEntity.desiredFeedback(
                    step.getDesiredFeedback().stream()
                            .map(df -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue(df.name()))
                            .collect(Collectors.toSet())
            );
        }

        return workflowEntity;
    }
}


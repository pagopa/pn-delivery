package it.pagopa.pn.delivery.rest.mapper;

import it.pagopa.pn.commons.db.campaign.entity.CampaignEntity;
import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignDetail;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CampaignSummary;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import it.pagopa.pn.delivery.models.internal.campaign.ChannelType;
import it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity;

import java.time.ZoneOffset;
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
                .taxonomyCode(campaign.getTaxonomyCode())
                .serviceName(campaign.getServiceName());

        List<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity> workflow = campaign.getWorkflow() == null
                ? Collections.emptyList()
                : campaign.getWorkflow().stream()
                .filter(Objects::nonNull)
                .map(CampaignMapper::toDtoWorkflowEntity)
                .toList();

        detail.workflow(workflow);
        return detail;
    }

    public static Campaign toInternalCampaign(CampaignEntity campaignEntity) {
        if (campaignEntity == null) {
            return null;
        }

        return Campaign.builder()
                .campaignId(campaignEntity.getCampaignId())
                .senderId(campaignEntity.getSenderId())
                .title(campaignEntity.getTitle())
                .descriptionScope(campaignEntity.getDescriptionScope())
                .startDate(campaignEntity.getStartDate() != null ? campaignEntity.getStartDate().atOffset(ZoneOffset.UTC) : null)
                .endDate(campaignEntity.getEndDate() != null ? campaignEntity.getEndDate().atOffset(ZoneOffset.UTC) : null)
                .status(campaignEntity.getStatus() == null ? null : it.pagopa.pn.delivery.models.internal.campaign.CampaignStatus.valueOf(campaignEntity.getStatus().name()))
                .senderContact(campaignEntity.getSenderContact())
                .serviceId(campaignEntity.getServiceId())
                .sensitiveContent(campaignEntity.getSensitiveContent())
                .stopOnViewed(campaignEntity.getStopOnViewed())
                .taxonomyCode(campaignEntity.getTaxonomyCode())
                .serviceName(campaignEntity.getServiceName())
                .workflow(campaignEntity.getWorkflow() == null
                        ? Collections.emptyList()
                        : campaignEntity.getWorkflow().stream()
                        .filter(Objects::nonNull)
                        .map(CampaignMapper::toInternalWorkflowEntity)
                        .toList())
                .build();
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

    // Mapping: Internal WorkFlowEntity -> OpenAPI DTO WorkflowEntity
    private static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity toDtoWorkflowEntity(WorkFlowEntity step) {
        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity dto =
                new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.WorkflowEntity()
                        .includeAttachment(step.getIncludeAttachment());

        if (step.getChannel() != null) {
            dto.channel(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ChannelType.fromValue(step.getChannel().name()));
        }

        if (step.getRecipientType() != null) {
            Set<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt> mappedRecipientTypes = step.getRecipientType().stream()
                    .filter(Objects::nonNull)
                    .map(type -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.RecipientTypeInt.fromValue(type.name()))
                    .collect(Collectors.toSet());

            dto.recipientType(mappedRecipientTypes);
        }

        if (step.getTimeout() != null) {
            dto.timeout(step.getTimeout().toString());
        }

        if (step.getDesiredFeedback() != null) {
            dto.desiredFeedback(
                    step.getDesiredFeedback().stream()
                            .filter(Objects::nonNull)
                            .map(df -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.DesiredFeedbackType.fromValue(df.name()))
                            .collect(Collectors.toSet())
            );
        }

        return dto;
    }

    // Mapping: Commons DB WorkflowEntity -> Internal WorkFlowEntity
    private static WorkFlowEntity toInternalWorkflowEntity(it.pagopa.pn.commons.db.campaign.entity.WorkflowEntity workflowEntity) {
        return WorkFlowEntity.builder()
                .channel(workflowEntity.getChannel() == null ? null : ChannelType.valueOf(workflowEntity.getChannel().name()))
                .recipientType(workflowEntity.getRecipientType() == null
                        ? Collections.emptySet()
                        : workflowEntity.getRecipientType().stream()
                        .filter(Objects::nonNull)
                        .map(type -> RecipientTypeInt.valueOf(type.name()))
                        .collect(Collectors.toSet()))
                .timeout(workflowEntity.getTimeout())
                .desiredFeedback(workflowEntity.getDesiredFeedback() == null
                        ? Collections.emptySet()
                        : workflowEntity.getDesiredFeedback().stream()
                        .filter(Objects::nonNull)
                        .map(df -> it.pagopa.pn.delivery.models.internal.campaign.DesiredFeedbackType.valueOf(df.name()))
                        .collect(Collectors.toSet()))
                .includeAttachment(workflowEntity.getIncludeAttachment())
                .build();
    }
}
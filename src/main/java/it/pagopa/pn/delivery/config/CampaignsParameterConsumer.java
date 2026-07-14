package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnCampaignNotFoundException;
import it.pagopa.pn.delivery.models.internal.campaign.Campaign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
public class CampaignsParameterConsumer {

    private static final String PARAMETER_STORE_MVP_CAMPAIGNS = "MVPCampaigns";

    private final ParameterConsumer parameterConsumer;
    private List<Campaign> campaigns = Collections.emptyList();

    public CampaignsParameterConsumer(ParameterConsumer parameterConsumer) {
        this.parameterConsumer = parameterConsumer;
    }

    @PostConstruct
    protected void initialize() {
        Optional<Campaign[]> maybeCampaigns = loadCampaigns();

        if (maybeCampaigns.isEmpty()) {
            log.info("No campaign configuration found on parameter store");
            return;
        }

        List<Campaign> loaded = new ArrayList<>();
        for (Campaign campaign : maybeCampaigns.get()) {
            if (isValid(campaign)) {
                log.info("Adding campaign configuration to in-memory load list campaignId={}, senderId={}",
                        campaign.getCampaignId(), campaign.getSenderId());
                loaded.add(campaign);
            } else {
                log.warn("Invalid campaign configuration found: {}", campaign);
            }
        }
        campaigns = Collections.unmodifiableList(loaded);

        log.info("Loaded {} campaigns in memory", campaigns.size());
    }

    private Optional<Campaign[]> loadCampaigns() {
        try {
            return parameterConsumer.getParameterValue(
                    PARAMETER_STORE_MVP_CAMPAIGNS,
                    Campaign[].class
            );
        } catch (PnInternalException ex) {
            if (hasParameterNotFoundCause(ex)) {
                log.info("Campaign configuration parameter {} not found on parameter store", PARAMETER_STORE_MVP_CAMPAIGNS);
                return Optional.empty();
            }
            throw ex;
        }
    }

    public List<Campaign> getCampaignsBySenderId(String senderId) {
        return campaigns.stream()
                .filter(campaign -> Objects.equals(senderId, campaign.getSenderId()))
                .toList();
    }

    public Campaign getCampaignByCampaignIdAndSenderId(String campaignId, String senderId) {
        return campaigns.stream()
                .filter(campaign -> Objects.equals(campaignId, campaign.getCampaignId())
                        && Objects.equals(senderId, campaign.getSenderId()))
                .findFirst()
                .orElseThrow(() -> new PnCampaignNotFoundException(
                        String.format("Campaign with campaignId=%s and senderId=%s not found", campaignId, senderId)
                ));
    }

    private boolean isValid(Campaign campaign) {
        return !Objects.isNull(campaign)
                && StringUtils.hasText(campaign.getCampaignId())
                && isValidSenderId(campaign.getSenderId())
                && StringUtils.hasText(campaign.getTitle())
                && StringUtils.hasText(campaign.getDescriptionScope())
                && !Objects.isNull(campaign.getStartDate())
                && !Objects.isNull(campaign.getEndDate())
                && !Objects.isNull(campaign.getStatus())
                && StringUtils.hasText(campaign.getServiceId())
                && hasValidWorkflow(campaign.getWorkflow());
    }

    private boolean isValidSenderId(String senderId) {
        if (!StringUtils.hasText(senderId)) {
            return false;
        }

        try {
            UUID.fromString(senderId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean hasValidWorkflow(List<it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity> workflow) {
        return !Objects.isNull(workflow)
                && workflow.stream().allMatch(this::hasValidWorkflowStep);
    }

    private boolean hasValidWorkflowStep(it.pagopa.pn.delivery.models.internal.campaign.WorkFlowEntity workflowStep) {
        return !Objects.isNull(workflowStep)
                && !Objects.isNull(workflowStep.getChannel())
                && !Objects.isNull(workflowStep.getRecipientType())
                && !workflowStep.getRecipientType().isEmpty()
                && workflowStep.getRecipientType().stream().allMatch(Objects::nonNull);
    }

    private boolean hasParameterNotFoundCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ParameterNotFoundException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

package it.pagopa.pn.delivery.models.internal.campaign;

import it.pagopa.pn.commons.utils.qr.models.RecipientTypeInt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Set;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkFlowEntity {
	private ChannelType channel;
	private Set<RecipientTypeInt> recipientType;
	private Duration timeout;
	private Set<DesiredFeedbackType> desiredFeedback;
	private Boolean includeAttachment;
}


package it.pagopa.pn.delivery.middleware.asseverationeventsproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.models.AsseverationEvent;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SqsAsseverationEventsProducer extends AbstractSqsMomProducer<AsseverationEvent> implements AsseverationEventsProducer {
    public SqsAsseverationEventsProducer(SqsClient sqsClient, ObjectMapper objectMapper, PnDeliveryConfigs cfg) {
        super(sqsClient, cfg.getTopics().getAsseverationEvents(), objectMapper, AsseverationEvent.class);
    }
}

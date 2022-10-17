package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SqsNotificationViewedProducer extends AbstractSqsMomProducer<PnDeliveryNotificationViewedEvent> implements NotificationViewedProducer {

    public SqsNotificationViewedProducer(SqsClient sqsClient, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(sqsClient, cfg.getTopics().getNewNotifications(), objectMapper, PnDeliveryNotificationViewedEvent.class);
    }

}

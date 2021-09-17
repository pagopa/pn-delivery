package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@ConditionalOnProperty( name = NotificationViewedProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.SQS )
public class SqsNotificationViewedProducer extends AbstractSqsMomProducer<PnDeliveryNotificationViewedEvent> implements NotificationViewedProducer {

    public SqsNotificationViewedProducer(SqsClient sqsClient, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(sqsClient, cfg.getTopics().getNotificationAcknowledgement(), objectMapper, PnDeliveryNotificationViewedEvent.class);
    }

}

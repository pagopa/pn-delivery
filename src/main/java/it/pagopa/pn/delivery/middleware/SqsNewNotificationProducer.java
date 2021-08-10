package it.pagopa.pn.delivery.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.AbstractSqsMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;


@Component
@ConditionalOnProperty( name = NewNotificationProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.SQS )
public class SqsNewNotificationProducer extends AbstractSqsMomProducer<NewNotificationEvent> implements NewNotificationProducer {

    public SqsNewNotificationProducer(SqsClient sqsClient, ObjectMapper objectMapper, ProducerConfigs cfg ) {
        super(sqsClient, cfg.getNewNotifications(), objectMapper, NewNotificationEvent.class);
    }

}

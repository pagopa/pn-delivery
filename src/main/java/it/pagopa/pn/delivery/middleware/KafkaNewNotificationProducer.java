package it.pagopa.pn.delivery.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.NewNotificationEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty( name = NewNotificationProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.KAFKA )
public class KafkaNewNotificationProducer extends AbstractKafkaMomProducer<NewNotificationEvent> implements NewNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaNewNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, ProducerConfigs cfg ) {
        super(kafkaTemplate, cfg.getNewnotifications(), objectMapper, NewNotificationEvent.class);
        this.kafkaTemplate = kafkaTemplate;
    }

}

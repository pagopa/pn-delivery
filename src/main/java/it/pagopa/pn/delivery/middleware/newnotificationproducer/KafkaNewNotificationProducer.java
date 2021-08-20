package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty( name = NewNotificationProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.KAFKA )
public class KafkaNewNotificationProducer extends AbstractKafkaMomProducer<PnDeliveryNewNotificationEvent> implements NewNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaNewNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(kafkaTemplate, cfg.getTopics().getNewNotifications(), objectMapper, PnDeliveryNewNotificationEvent.class);
        this.kafkaTemplate = kafkaTemplate;
    }

}

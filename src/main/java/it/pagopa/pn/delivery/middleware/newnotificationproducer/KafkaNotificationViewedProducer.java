package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;

@Component
@ConditionalOnProperty( name = NotificationViewedProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.KAFKA )
public class KafkaNotificationViewedProducer extends AbstractKafkaMomProducer<PnDeliveryNotificationViewedEvent> implements NotificationViewedProducer {

    public KafkaNotificationViewedProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(kafkaTemplate, cfg.getTopics().getNewNotifications(), objectMapper, PnDeliveryNotificationViewedEvent.class);
    }

}

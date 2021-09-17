package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty( name = NotificationViewedProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.KAFKA )
public class KafkaNotificationViewedProducer extends AbstractKafkaMomProducer<PnDeliveryNotificationViewedEvent> implements NotificationViewedProducer {

    public KafkaNotificationViewedProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(kafkaTemplate, cfg.getTopics().getNotificationAcknowledgement(), objectMapper, PnDeliveryNotificationViewedEvent.class);
    }

}

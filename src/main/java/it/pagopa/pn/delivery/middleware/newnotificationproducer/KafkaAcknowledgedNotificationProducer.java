package it.pagopa.pn.delivery.middleware.newnotificationproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationAcknowledgementEvent;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.NewNotificationProducer;
import it.pagopa.pn.delivery.middleware.NotificationAcknowledgementProducer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty( name = NotificationAcknowledgementProducer.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.KAFKA )
public class KafkaAcknowledgedNotificationProducer extends AbstractKafkaMomProducer<PnDeliveryNotificationAcknowledgementEvent> implements NotificationAcknowledgementProducer {

    public KafkaAcknowledgedNotificationProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, PnDeliveryConfigs cfg ) {
        super(kafkaTemplate, cfg.getTopics().getNotificationAcknowledgement(), objectMapper, PnDeliveryNotificationAcknowledgementEvent.class);
    }

}

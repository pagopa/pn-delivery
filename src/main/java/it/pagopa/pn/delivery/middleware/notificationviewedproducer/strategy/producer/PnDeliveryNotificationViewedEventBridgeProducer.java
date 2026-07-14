package it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.PnDeliveryNotificationViewedEvent;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.middleware.eventbridge.AbstractEventBridgeProducer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Component
public class PnDeliveryNotificationViewedEventBridgeProducer
        extends AbstractEventBridgeProducer<PnDeliveryNotificationViewedEvent> {

    public PnDeliveryNotificationViewedEventBridgeProducer(
            EventBridgeClient amazonEventBridge,
            PnDeliveryConfigs configs,
            ObjectMapper objectMapper) {
        super(
                amazonEventBridge,
                configs.getEventBridge().getSource(),
                configs.getEventBridge().getNotificationViewedDetailType(),
                configs.getEventBridge().getName(),
                objectMapper
        );
    }
}
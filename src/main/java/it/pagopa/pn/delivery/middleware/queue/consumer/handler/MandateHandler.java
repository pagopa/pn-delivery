package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class MandateHandler {

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryAcceptedMandateConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryAcceptMandateConsumer - message: {}", message);
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRejectedMandateConsumer() {
        return message -> {

        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRevokedMandateConsumer() {
        return message -> {

        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryExpiredMandateConsumer() {
        return message -> {

        };
    }

}

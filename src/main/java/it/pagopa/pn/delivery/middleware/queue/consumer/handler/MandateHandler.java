package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.delivery.svc.NotificationDelegatedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class MandateHandler {

    private final NotificationDelegatedService notificationDelegatedService;

    public MandateHandler(NotificationDelegatedService notificationDelegatedService) {
        this.notificationDelegatedService = notificationDelegatedService;
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryAcceptedMandateConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryAcceptMandateConsumer - message: {}", message);
                notificationDelegatedService.handleAcceptedMandate(message.getPayload(), EventType.MANDATE_ACCEPTED);
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRejectedMandateConsumer() {
        return message ->  {
            try {
                log.debug("pnDeliveryRejectedMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REJECTED);
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRevokedMandateConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryRevokedMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REVOKED);
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryExpiredMandateConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryExpiredMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_EXPIRED);
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

}

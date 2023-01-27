package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

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
    private static final String  MANDATE_REJECTED = "reject";
    private static final String  MANDATE_REVOKED = "revoke";
    private static final String  MANDATE_EXPIRED = "expired";

    private final NotificationDelegatedService notificationDelegatedService;

    public MandateHandler(NotificationDelegatedService notificationDelegatedService) {
        this.notificationDelegatedService = notificationDelegatedService;
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryAcceptedMandateConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryAcceptMandateConsumer - message: {}", message);
                notificationDelegatedService.handleAcceptedMandate(message.getPayload());
            } catch (Exception e) {
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRejectedMandateConsumer() {
        return message -> this.notificationDelegatedService
                .deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), MANDATE_REJECTED);
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRevokedMandateConsumer() {
        return message -> this.notificationDelegatedService
                .deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), MANDATE_REVOKED);
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryExpiredMandateConsumer() {
        return message -> this.notificationDelegatedService
                .deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), MANDATE_EXPIRED);
    }

}

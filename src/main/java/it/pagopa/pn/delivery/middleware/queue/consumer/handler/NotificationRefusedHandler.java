package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import it.pagopa.pn.delivery.svc.PaNotificationLimitService;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@CustomLog
@Configuration
public class NotificationRefusedHandler {

    private static final String NOTIFICATION_REFUSED_HANDLER_PROCESS = "pnDeliveryNotificationRefusedConsumer";

    private final PaNotificationLimitService paNotificationLimitService;

    public NotificationRefusedHandler(PaNotificationLimitService paNotificationLimitService) {
        this.paNotificationLimitService = paNotificationLimitService;
    }

    @Bean
    public Consumer<Message<NotificationRefusedPayload>> pnDeliveryNotificationRefusedConsumer() {
        return message -> {
            try {
                log.logStartingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS);
                log.debug("pnDeliveryNotificationRefusedConsumer - message: {}", message);
                paNotificationLimitService.handleNotificationRefused(message.getPayload());
                log.logEndingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS, false, e.getMessage());
                throw e;
            }
        };
    }

}

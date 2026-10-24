package it.pagopa.pn.delivery.middleware.queue.consumer;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.delivery.models.NotificationRefusedPayload;
import it.pagopa.pn.delivery.svc.PaNotificationLimitService;
import lombok.CustomLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.delivery.middleware.queue.consumer.utils.HandleEventUtils.setMdc;

@CustomLog
@Configuration
public class NotificationRefusedConsumer {

    private static final String NOTIFICATION_REFUSED_HANDLER_PROCESS = "pnDeliveryNotificationRefusedConsumer";

    private final PaNotificationLimitService paNotificationLimitService;

    public NotificationRefusedConsumer(PaNotificationLimitService paNotificationLimitService) {
        this.paNotificationLimitService = paNotificationLimitService;
    }

    @SqsListener(value = "${pn.delivery.topics.notification.refused}")
    public void consume(Message<NotificationRefusedPayload> message) {
        setMdc(message);
        try {
            log.logStartingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS);
            log.debug("pnDeliveryNotificationRefusedConsumer - message: {}", message);
            paNotificationLimitService.handleNotificationRefused(message.getPayload());
            log.logEndingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(NOTIFICATION_REFUSED_HANDLER_PROCESS, false, e.getMessage(), e);
            throw e;
        }
    }

}

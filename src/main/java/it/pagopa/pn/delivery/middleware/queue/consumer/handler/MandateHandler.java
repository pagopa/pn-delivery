package it.pagopa.pn.delivery.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.delivery.svc.NotificationDelegatedService;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@CustomLog
@Configuration
public class MandateHandler {

    private static final String ACCEPT_HANDLER_PROCESS = "pnDeliveryAcceptedMandateConsumer";
    private static final String UPDATE_HANDLER_PROCESS = "pnDeliveryUpdatedMandateConsumer";
    private static final String REJECTED_HANDLER_PROCESS = "pnDeliveryRejectedMandateConsumer";
    private static final String REVOKED_HANDLER_PROCESS = "pnDeliveryRevokedMandateConsumer";
    private static final String EXPIRED_HANDLER_PROCESS = "pnDeliveryExpiredMandateConsumer";

    private final NotificationDelegatedService notificationDelegatedService;

    public MandateHandler(NotificationDelegatedService notificationDelegatedService) {
        this.notificationDelegatedService = notificationDelegatedService;
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryAcceptedMandateConsumer() {
        return message -> {
            try {
                log.logStartingProcess(ACCEPT_HANDLER_PROCESS);
                log.debug("pnDeliveryAcceptMandateConsumer - message: {}", message);
                notificationDelegatedService.handleAcceptedMandate(message.getPayload(), EventType.MANDATE_ACCEPTED);
                log.logEndingProcess(ACCEPT_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(ACCEPT_HANDLER_PROCESS, false, e.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryUpdatedMandateConsumer() {
        return message -> {
            try {
                log.logStartingProcess(UPDATE_HANDLER_PROCESS);
                log.debug("pnDeliveryUpdatedMandateConsumer - message: {}", message);
                notificationDelegatedService.handleUpdatedMandate(message.getPayload(), EventType.MANDATE_UPDATED);
                log.logEndingProcess(UPDATE_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(UPDATE_HANDLER_PROCESS, false, e.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRejectedMandateConsumer() {
        return message ->  {
            try {
                log.logStartingProcess(REJECTED_HANDLER_PROCESS);
                log.debug("pnDeliveryRejectedMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REJECTED);
                log.logEndingProcess(REJECTED_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(REJECTED_HANDLER_PROCESS, false, e.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryRevokedMandateConsumer() {
        return message -> {
            try {
                log.logStartingProcess(REVOKED_HANDLER_PROCESS);
                log.debug("pnDeliveryRevokedMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REVOKED);
                log.logEndingProcess(REVOKED_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(REVOKED_HANDLER_PROCESS, false, e.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

    @Bean
    public Consumer<Message<PnMandateEvent.Payload>> pnDeliveryExpiredMandateConsumer() {
        return message -> {
            try {
                log.logStartingProcess(EXPIRED_HANDLER_PROCESS);
                log.debug("pnDeliveryExpiredMandateConsumer - message: {}", message);
                notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_EXPIRED);
                log.logEndingProcess(EXPIRED_HANDLER_PROCESS);
            } catch (Exception e) {
                log.logEndingProcess(EXPIRED_HANDLER_PROCESS, false, e.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), e);
                throw e;
            }
        };
    }

}

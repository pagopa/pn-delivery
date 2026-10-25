package it.pagopa.pn.delivery.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.PnMandateEvent;
import it.pagopa.pn.delivery.middleware.queue.consumer.utils.HandleEventUtils;
import it.pagopa.pn.delivery.svc.NotificationDelegatedService;
import lombok.CustomLog;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import static it.pagopa.pn.delivery.middleware.queue.consumer.utils.HandleEventUtils.setMdc;

@CustomLog
@Configuration
public class MandateConsumer {

    private static final String ACCEPT_HANDLER_PROCESS = "pnDeliveryAcceptedMandateConsumer";
    private static final String UPDATE_HANDLER_PROCESS = "pnDeliveryUpdatedMandateConsumer";
    private static final String REJECTED_HANDLER_PROCESS = "pnDeliveryRejectedMandateConsumer";
    private static final String REVOKED_HANDLER_PROCESS = "pnDeliveryRevokedMandateConsumer";
    private static final String EXPIRED_HANDLER_PROCESS = "pnDeliveryExpiredMandateConsumer";

    private final NotificationDelegatedService notificationDelegatedService;

    public MandateConsumer(NotificationDelegatedService notificationDelegatedService) {
        this.notificationDelegatedService = notificationDelegatedService;
    }

    @SqsListener(value = "${pn.delivery.sqs.from.pn.mandate.name}")
    public void consume(Message<PnMandateEvent.Payload> message) {
        setMdc(message);
        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("Received message from SQS with eventType: {}", eventType);

        if(eventType == null) {
            log.error("Missing required eventType header");
            HandleEventUtils.handleException(message.getHeaders(), new IllegalArgumentException("Missing required eventType header"));
            return;
        }

        switch (eventType) {
            case "MANDATE_ACCEPTED":
                handleAcceptedMandate(message);
            break;
            case "MANDATE_UPDATED":
                handleUpdatedMandate(message);
            break;
            case "MANDATE_REJECTED":
                handleRejectedMandate(message);
            break;
            case "MANDATE_REVOKED":
                handleRevokedMandate(message);
            break;
            case "MANDATE_EXPIRED":
                handleExpiredMandate(message);
            break;
            default:
                log.error("Undefined handler for eventType: {}", eventType);
                HandleEventUtils.handleException(message.getHeaders(), new IllegalArgumentException("Undefined handler for eventType: " + eventType));
        }
    }

    public void handleAcceptedMandate(Message<PnMandateEvent.Payload> message) {
        try {
            log.logStartingProcess(ACCEPT_HANDLER_PROCESS);
            log.debug("pnDeliveryAcceptMandateConsumer - message: {}", message);
            notificationDelegatedService.handleAcceptedMandate(message.getPayload(), EventType.MANDATE_ACCEPTED);
            log.logEndingProcess(ACCEPT_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(ACCEPT_HANDLER_PROCESS, false, e.getMessage(), e);
            HandleEventUtils.handleException(message.getHeaders(), e);
            throw e;
        }
    }

    public void handleUpdatedMandate(Message<PnMandateEvent.Payload> message) {
        try {
            log.logStartingProcess(UPDATE_HANDLER_PROCESS);
            log.debug("pnDeliveryUpdatedMandateConsumer - message: {}", message);
            notificationDelegatedService.handleUpdatedMandate(message.getPayload(), EventType.MANDATE_UPDATED);
            log.logEndingProcess(UPDATE_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(UPDATE_HANDLER_PROCESS, false, e.getMessage(), e);
            HandleEventUtils.handleException(message.getHeaders(), e);
            throw e;
        }
    }

    public void handleRejectedMandate(Message<PnMandateEvent.Payload> message) {
        try {
            log.logStartingProcess(REJECTED_HANDLER_PROCESS);
            log.debug("pnDeliveryRejectedMandateConsumer - message: {}", message);
            notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REJECTED);
            log.logEndingProcess(REJECTED_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(REJECTED_HANDLER_PROCESS, false, e.getMessage(), e);
            HandleEventUtils.handleException(message.getHeaders(), e);
            throw e;
        }
    }

    public void handleRevokedMandate(Message<PnMandateEvent.Payload> message) {
        try {
            log.logStartingProcess(REVOKED_HANDLER_PROCESS);
            log.debug("pnDeliveryRevokedMandateConsumer - message: {}", message);
            notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_REVOKED);
            log.logEndingProcess(REVOKED_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(REVOKED_HANDLER_PROCESS, false, e.getMessage(), e);
            HandleEventUtils.handleException(message.getHeaders(), e);
            throw e;
        }
    }

    public void handleExpiredMandate(Message<PnMandateEvent.Payload> message) {
        try {
            log.logStartingProcess(EXPIRED_HANDLER_PROCESS);
            log.debug("pnDeliveryExpiredMandateConsumer - message: {}", message);
            notificationDelegatedService.deleteNotificationDelegatedByMandateId(message.getPayload().getMandateId(), EventType.MANDATE_EXPIRED);
            log.logEndingProcess(EXPIRED_HANDLER_PROCESS);
        } catch (Exception e) {
            log.logEndingProcess(EXPIRED_HANDLER_PROCESS, false, e.getMessage(), e);
            HandleEventUtils.handleException(message.getHeaders(), e);
            throw e;
        }
    }

}

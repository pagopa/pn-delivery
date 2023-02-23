package it.pagopa.pn.delivery.middleware.queue.consumer;

import it.pagopa.pn.commons.log.MDCWebFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Configuration
public class PnEventInboundService {

    private final EventHandler eventHandler;

    public PnEventInboundService(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Bean
    public MessageRoutingCallback customRouter() {
        return new MessageRoutingCallback() {
            @Override
            public FunctionRoutingResult routingResult(Message<?> message) {
                setTraceId(message);
                return new FunctionRoutingResult(handleMessage(message));
            }
        };
    }

    private void setTraceId(Message<?> message) {
        MessageHeaders headers = message.getHeaders();

        String traceId;

        if (headers.containsKey("aws_messageId")) {
            traceId = headers.get("aws_messageId", String.class);
        } else {
            traceId = "trace_id:" + UUID.randomUUID();
        }

        MDC.put(MDCWebFilter.MDC_TRACE_ID_KEY, traceId);
    }

    private String handleMessage(Message<?> message) {
        log.debug("received message from customRouter: {}", message);

        String eventType = (String) message.getHeaders().get("eventType");
        log.debug("received message from customRouter with eventType: {}", eventType);

        String handlerName = eventHandler.getHandler().get(eventType);
        if (!StringUtils.hasText(handlerName)) {
            log.error("undefined handler for eventType: {}", eventType);
        }
        return handlerName;
    }

}

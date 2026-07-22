package it.pagopa.pn.delivery.middleware.eventbridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.GenericEventBridgeEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_SEND_EVENT_BRIDGE_ERROR;

@Slf4j
@Component
public abstract class AbstractEventBridgeProducer<T extends GenericEventBridgeEvent> implements EventBridgeProducer<T> {

    private final EventBridgeClient amazonEventBridge;
    private final String eventBusName;
    private final String eventBusDetailType;
    private final String eventBusSource;
    private final ObjectMapper objectMapper;

    protected AbstractEventBridgeProducer(
            EventBridgeClient amazonEventBridge,
            String eventBusSource,
            String detailType,
            String name,
            ObjectMapper objectMapper
    ) {
        this.amazonEventBridge = amazonEventBridge;
        this.eventBusSource = eventBusSource;
        this.eventBusName = name;
        this.eventBusDetailType = detailType;
        this.objectMapper = objectMapper;
    }

    private PutEventsRequest putEventsRequestBuilder(List<T> events) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder()
                .entries(events.stream()
                        .map(this::buildEventRequest)
                        .toList()
                )
                .build();

        log.debug("PutEventsRequest: {}", putEventsRequest);
        return putEventsRequest;
    }

    private PutEventsRequestEntry buildEventRequest(T event) {
        return PutEventsRequestEntry.builder()
                .eventBusName(eventBusName)
                .detailType(eventBusDetailType)
                .source(eventBusSource)
                .detail(serializeDetail(event))
                .build();
    }

    private String serializeDetail(T event) {
        try {
            return objectMapper.writeValueAsString(event.getDetail());
        } catch (JsonProcessingException e) {
            throw new PnInternalException(
                    String.format("Error serializing event detail for event bus: %s", eventBusName), ERROR_CODE_DELIVERY_SEND_EVENT_BRIDGE_ERROR, e
            );
        }
    }

    @Override
    public void sendEvent(T event) {
        sendEvent(List.of(event));
    }

    @Override
    public void sendEvent(List<T> events) {
        PutEventsResponse response = amazonEventBridge.putEvents(putEventsRequestBuilder(events));
        if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
            response.entries().forEach(entry ->
                    log.error("EventBridge failed entry: errorCode={}, errorMessage={}",
                            entry.errorCode(), entry.errorMessage())
            );
            log.error("error sending event on event bus={} response={}", eventBusName, response);
            throw new PnInternalException(
                    String.format("Error sending event on event bus: %s", eventBusName), ERROR_CODE_DELIVERY_SEND_EVENT_BRIDGE_ERROR
            );
        }
        log.debug("Event sent successfully: {}", response.entries());
    }
}

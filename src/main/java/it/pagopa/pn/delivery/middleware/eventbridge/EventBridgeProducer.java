package it.pagopa.pn.delivery.middleware.eventbridge;

import java.util.Collections;
import java.util.List;

public interface EventBridgeProducer<T> {
    default void sendEvent(T event) {
        this.sendEvent(Collections.singletonList(event));
    }

    void sendEvent(List<T> events);
}
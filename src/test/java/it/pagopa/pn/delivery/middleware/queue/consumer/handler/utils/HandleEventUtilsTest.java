package it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.*;
import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_PUBLISHER;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_HANDLEEVENTFAILED;
import static org.junit.jupiter.api.Assertions.*;

class HandleEventUtilsTest {

    @Test
    void testMapStandardEventHeader() {
        StandardEventHeader actual = HandleEventUtils.mapStandardEventHeader(buildMessageHeaders());
        assertEquals(buildStandardEventHeader(), actual);
    }

    @Test
    void testMapStandardEventHeaderException() {
        PnInternalException pnInternalException = assertThrows(PnInternalException.class,
                () -> HandleEventUtils.mapStandardEventHeader(null));
        assertEquals(ERROR_CODE_DELIVERY_HANDLEEVENTFAILED, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void testHandleException() {
        assertDoesNotThrow(() -> HandleEventUtils.handleException(buildMessageHeaders(), new RuntimeException()));
        assertDoesNotThrow(() -> HandleEventUtils.handleException(null, new RuntimeException()));
    }

    private MessageHeaders buildMessageHeaders() {
        Map<String, Object> map = new HashMap<>();
        map.put(PN_EVENT_HEADER_EVENT_ID, "001");
        map.put(PN_EVENT_HEADER_EVENT_TYPE, "002");
        map.put(PN_EVENT_HEADER_PUBLISHER, "003");
        return new MessageHeaders(map);
    }

    private StandardEventHeader buildStandardEventHeader() {
        return StandardEventHeader.builder()
                .eventId("001")
                .eventType("002")
                .publisher("003")
                .build();
    }

}
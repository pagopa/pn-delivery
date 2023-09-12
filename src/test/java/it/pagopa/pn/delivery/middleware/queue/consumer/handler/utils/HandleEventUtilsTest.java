package it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.*;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_HANDLEEVENTFAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HandleEventUtilsTest {

    @Test
    void testMapStandardEventHeader() {
        StandardEventHeader actual = HandleEventUtils.mapStandardEventHeader(buildMessageHeaders());
        assertEquals(buildStandardEventHeader(), actual);
    }

    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader2() {
        assertThrows(PnInternalException.class, () -> HandleEventUtils.mapStandardEventHeader(null));
    }


    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader4() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenReturn(null);
        StandardEventHeader actualMapStandardEventHeaderResult = HandleEventUtils.mapStandardEventHeader(headers);
        assertNull(actualMapStandardEventHeaderResult.getCreatedAt());
        assertNull(actualMapStandardEventHeaderResult.getPublisher());
        assertNull(actualMapStandardEventHeaderResult.getIun());
        assertNull(actualMapStandardEventHeaderResult.getEventType());
        assertNull(actualMapStandardEventHeaderResult.getEventId());
        verify(headers, atLeast(1)).get(Mockito.<Object>any());
    }

    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader5() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenThrow(new PnInternalException("An error occurred"));
        assertThrows(PnInternalException.class, () -> HandleEventUtils.mapStandardEventHeader(headers));
        verify(headers).get(Mockito.<Object>any());
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


    /**
     * Method under test: {@link HandleEventUtils#handleException(MessageHeaders, Exception)}
     */
    @Test
    void testHandleException4() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenReturn(null);
        HandleEventUtils.handleException(headers, new Exception("foo"));
        verify(headers, atLeast(1)).get(Mockito.<Object>any());
    }

    /**
     * Method under test: {@link HandleEventUtils#handleException(MessageHeaders, Exception)}
     */
    @Test
    void testHandleException5() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenThrow(new PnInternalException("An error occurred"));
        assertThrows(PnInternalException.class, () -> HandleEventUtils.handleException(headers, new Exception("foo")));
        verify(headers).get(Mockito.<Object>any());
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
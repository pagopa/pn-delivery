package it.pagopa.pn.delivery.middleware.queue.consumer.handler.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static it.pagopa.pn.api.dto.events.GenericEventHeader.*;
import static it.pagopa.pn.api.dto.events.GenericEventHeader.PN_EVENT_HEADER_PUBLISHER;
import static it.pagopa.pn.api.dto.events.StandardEventHeader.PN_EVENT_HEADER_IUN;
import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_HANDLEEVENTFAILED;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HandleEventUtils {

    public static void handleException(MessageHeaders headers, Exception e) {
        if (headers != null) {
            StandardEventHeader standardEventHeader = mapStandardEventHeader(headers);
            log.error("generic exception from publisher {}", standardEventHeader.getPublisher(), e);
        } else {
            log.error("generic exception", e);
        }
    }

    public static StandardEventHeader mapStandardEventHeader(MessageHeaders headers) {
        if (headers != null) {
            return StandardEventHeader.builder()
                    .eventId((String) headers.get(PN_EVENT_HEADER_EVENT_ID))
                    .iun((String) headers.get(PN_EVENT_HEADER_IUN))
                    .eventType((String) headers.get(PN_EVENT_HEADER_EVENT_TYPE))
                    .createdAt(mapInstant(headers.get(PN_EVENT_HEADER_CREATED_AT)))
                    .publisher((String) headers.get(PN_EVENT_HEADER_PUBLISHER))
                    .build();
        } else {
            String msg = "Headers cannot be null in mapStandardEventHeader";
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_DELIVERY_HANDLEEVENTFAILED);
        }
    }

    private static Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }

}

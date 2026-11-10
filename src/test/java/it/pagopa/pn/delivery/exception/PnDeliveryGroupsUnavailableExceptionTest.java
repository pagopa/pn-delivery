package it.pagopa.pn.delivery.exception;

import org.junit.jupiter.api.Test;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_GROUPS_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PnDeliveryGroupsUnavailableExceptionTest {

    @Test
    void shouldExposeBadGatewayStatusAndDedicatedErrorCode() {
        String description = "Unable to retrieve groups for senderId=senderId";

        PnDeliveryGroupsUnavailableException exception =
                new PnDeliveryGroupsUnavailableException(description);

        assertEquals(502, exception.getStatus());
        assertEquals(502, exception.getProblem().getStatus().intValue());
        assertEquals("Unable to retrieve PA groups", exception.getProblem().getTitle());
        assertEquals(description, exception.getProblem().getDetail());
        assertEquals(1, exception.getProblem().getErrors().size());
        assertEquals(ERROR_CODE_DELIVERY_GROUPS_UNAVAILABLE,
                exception.getProblem().getErrors().get(0).getCode());
    }
}

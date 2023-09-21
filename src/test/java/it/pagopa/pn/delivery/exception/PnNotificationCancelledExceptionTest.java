package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnNotificationCancelledExceptionTest {
    /**
     * Method under test: {@link PnNotificationCancelledException#PnNotificationCancelledException(String, Exception)}
     */
    @Test
    void test() {
        PnNotificationCancelledException actualPnNotificationCancelledException = new PnNotificationCancelledException(
                "The characteristics of someone or something", new Exception("foo"));

        assertEquals(404, actualPnNotificationCancelledException.getStatus());
        Problem problem = actualPnNotificationCancelledException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification cancelled", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_CANCELLED, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

}


package it.pagopa.pn.delivery.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;

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

    /**
     * Method under test: {@link PnNotificationCancelledException#PnNotificationCancelledException(String, Exception)}
     */
    @Test
    void testConstructor2() {
        PnNotificationCancelledException actualPnNotificationCancelledException = new PnNotificationCancelledException(
                "foo", new Exception("foo"));

        assertEquals(404, actualPnNotificationCancelledException.getStatus());
        Problem problem = actualPnNotificationCancelledException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification cancelled", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("foo", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_CANCELLED, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationCancelledException#PnNotificationCancelledException(String, Exception)}
     */
    @Test
    void testConstructor3() {
        PnNotificationCancelledException actualPnNotificationCancelledException = new PnNotificationCancelledException("",
                new Exception("foo"));

        assertEquals(404, actualPnNotificationCancelledException.getStatus());
        Problem problem = actualPnNotificationCancelledException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification cancelled", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATION_CANCELLED, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }
}


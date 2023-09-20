package it.pagopa.pn.delivery.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;

class PnNotificationNotFoundExceptionTest {

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String)}
     */
    @Test
    void testConstructor() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException(
                "The characteristics of someone or something");
        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification not found", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String)}
     */
    @Test
    void testConstructor2() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException(
                "foo");
        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification not found", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("foo", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String)}
     */
    @Test
    void testConstructor3() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException("");
        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Notification not found", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_NOTIFICATIONNOTFOUND, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor4() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException(
                "An error occurred", "The characteristics of someone or something", "An error occurred",
                new Exception("foo"));

        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor5() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException("",
                "The characteristics of someone or something", "An error occurred", new Exception("foo"));

        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnNotificationNotFoundException#PnNotificationNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor6() {
        PnNotificationNotFoundException actualPnNotificationNotFoundException = new PnNotificationNotFoundException(
                "An error occurred", "", "An error occurred", new Exception("foo"));

        assertEquals(404, actualPnNotificationNotFoundException.getStatus());
        Problem problem = actualPnNotificationNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }
}


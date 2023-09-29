package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnBadRequestExceptionTest {
    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String)}
     */
    @Test
    void testConstructor() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred",
                "The characteristics of someone or something", "An error occurred");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
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
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String)}
     */
    @Test
    void testConstructor5() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("foo", "foo", "foo");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("foo", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("foo", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("foo", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String)}
     */
    @Test
    void testConstructor6() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("",
                "The characteristics of someone or something", "An error occurred");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
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
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String)}
     */
    @Test
    void testConstructor7() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred", "",
                "An error occurred");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, Exception)}
     */
    @Test
    void testConstructor8() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred",
                "The characteristics of someone or something", "An error occurred", new Exception("foo"));

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
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
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, Exception)}
     */
    @Test
    void testConstructor12() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("",
                "The characteristics of someone or something", "An error occurred", new Exception("foo"));

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
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
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, Exception)}
     */
    @Test
    void testConstructor13() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred", "",
                "An error occurred", new Exception("foo"));

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, String)}
     */
    @Test
    void testConstructor14() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred",
                "The characteristics of someone or something", "An error occurred", "Detail");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("Detail", getResult.getDetail());
    }


    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, String)}
     */
    @Test
    void testConstructor18() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("",
                "The characteristics of someone or something", "An error occurred", "Detail");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("Detail", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnBadRequestException#PnBadRequestException(String, String, String, String)}
     */
    @Test
    void testConstructor19() {
        PnBadRequestException actualPnBadRequestException = new PnBadRequestException("An error occurred", "",
                "An error occurred", "Detail");

        assertEquals(400, actualPnBadRequestException.getStatus());
        Problem problem = actualPnBadRequestException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("An error occurred", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("Detail", getResult.getDetail());
    }
}


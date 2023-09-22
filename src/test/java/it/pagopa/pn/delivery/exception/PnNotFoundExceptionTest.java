package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnNotFoundExceptionTest {

    /**
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String)}
     */
    @Test
    void testConstructor() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("An error occurred",
                "The characteristics of someone or something", "An error occurred");

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String)}
     */
    @Test
    void testConstructor5() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("foo", "foo", "foo");

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("foo", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
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
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String)}
     */
    @Test
    void testConstructor6() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("",
                "The characteristics of someone or something", "An error occurred");

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String)}
     */
    @Test
    void testConstructor7() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("An error occurred", "",
                "An error occurred");

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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

    /**
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor8() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("An error occurred",
                "The characteristics of someone or something", "An error occurred", new Exception("foo"));

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor12() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("",
                "The characteristics of someone or something", "An error occurred", new Exception("foo"));

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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
     * Method under test: {@link PnNotFoundException#PnNotFoundException(String, String, String, Exception)}
     */
    @Test
    void testConstructor13() {
        PnNotFoundException actualPnNotFoundException = new PnNotFoundException("An error occurred", "",
                "An error occurred", new Exception("foo"));

        assertEquals(404, actualPnNotFoundException.getStatus());
        Problem problem = actualPnNotFoundException.getProblem();
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


package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnInvalidInputExceptionTest {
    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, String)}
     */
    @Test
    void testConstructor() {
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred", "Field");

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Input non valido", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertEquals("Field", getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, String)}
     */
    @Test
    void testConstructor2() {
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("foo", "foo");

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Input non valido", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("foo", getResult.getCode());
        assertEquals("foo", getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, String, String)}
     */
    @Test
    void testConstructor3() {
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred", "Field",
                "Diagnostic");

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Input non valido", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertEquals("Field", getResult.getElement());
        assertEquals("Diagnostic", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, List)}
     */
    @Test
    void testConstructor4() {
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred",
                new ArrayList<>());

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("An error occurred", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult = errors.get(0);
        assertEquals("PN_GENERIC_ERROR", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, List)}
     */
    @Test
    void testConstructor5() {
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("", new ArrayList<>());

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult = errors.get(0);
        assertEquals("PN_GENERIC_ERROR", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, List)}
     */
    @Test
    void testConstructor6() {
        ArrayList<it.pagopa.pn.commons.exceptions.dto.ProblemError> problemErrorList = new ArrayList<>();
        problemErrorList
                .add(new it.pagopa.pn.commons.exceptions.dto.ProblemError("GENERIC_ERROR", "GENERIC_ERROR", "GENERIC_ERROR"));
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred",
                problemErrorList);

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("An error occurred", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult = errors.get(0);
        assertEquals("GENERIC_ERROR", getResult.getCode());
        assertEquals("GENERIC_ERROR", getResult.getElement());
        assertEquals("GENERIC_ERROR", getResult.getDetail());
        assertEquals("GENERIC_ERROR", problemErrorList.get(0).getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, List)}
     */
    @Test
    void testConstructor7() {
        ArrayList<it.pagopa.pn.commons.exceptions.dto.ProblemError> problemErrorList = new ArrayList<>();
        problemErrorList
                .add(new it.pagopa.pn.commons.exceptions.dto.ProblemError("GENERIC_ERROR", "GENERIC_ERROR", "GENERIC_ERROR"));
        problemErrorList
                .add(new it.pagopa.pn.commons.exceptions.dto.ProblemError("GENERIC_ERROR", "GENERIC_ERROR", "GENERIC_ERROR"));
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred",
                problemErrorList);

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors = problem.getErrors();
        assertEquals(2, errors.size());
        assertEquals("An error occurred", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult = errors.get(0);
        assertEquals("GENERIC_ERROR", getResult.getElement());
        assertEquals("GENERIC_ERROR", getResult.getDetail());
        assertEquals("GENERIC_ERROR", getResult.getCode());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult2 = errors.get(1);
        assertEquals("GENERIC_ERROR", getResult2.getElement());
        assertEquals("GENERIC_ERROR", getResult2.getDetail());
        assertEquals("GENERIC_ERROR", getResult2.getCode());
        assertEquals("GENERIC_ERROR", problemErrorList.get(1).getDetail());
        assertEquals("GENERIC_ERROR", problemErrorList.get(0).getDetail());
    }

    /**
     * Method under test: {@link PnInvalidInputException#PnInvalidInputException(String, List)}
     */
    @Test
    void testConstructor8() {
        ArrayList<it.pagopa.pn.commons.exceptions.dto.ProblemError> problemErrorList = new ArrayList<>();
        problemErrorList.add(new it.pagopa.pn.commons.exceptions.dto.ProblemError());
        PnInvalidInputException actualPnInvalidInputException = new PnInvalidInputException("An error occurred",
                problemErrorList);

        assertEquals(400, actualPnInvalidInputException.getStatus());
        Problem problem = actualPnInvalidInputException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(400, problem.getStatus().intValue());
        List<it.pagopa.pn.common.rest.error.v1.dto.ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("An error occurred", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        it.pagopa.pn.common.rest.error.v1.dto.ProblemError getResult = errors.get(0);
        assertNull(getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
        assertEquals("none", problemErrorList.get(0).getDetail());
    }

}


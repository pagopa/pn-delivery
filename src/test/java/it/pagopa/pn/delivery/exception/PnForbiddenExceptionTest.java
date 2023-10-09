package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnForbiddenExceptionTest {

    /**
     * Method under test: {@link PnForbiddenException#PnForbiddenException(String)}
     */
    @Test
    void testConstructor() {
        PnForbiddenException actualPnForbiddenException = new PnForbiddenException("An error occurred");
        assertEquals(404, actualPnForbiddenException.getStatus());
        Problem problem = actualPnForbiddenException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Accesso negato!", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("L'utente non è autorizzato ad accedere alla risorsa richiesta.", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnForbiddenException#PnForbiddenException(String)}
     */
    @Test
    void testConstructor3() {
        PnForbiddenException actualPnForbiddenException = new PnForbiddenException("foo");
        assertEquals(404, actualPnForbiddenException.getStatus());
        Problem problem = actualPnForbiddenException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Accesso negato!", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("L'utente non è autorizzato ad accedere alla risorsa richiesta.", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals("foo", getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }
}


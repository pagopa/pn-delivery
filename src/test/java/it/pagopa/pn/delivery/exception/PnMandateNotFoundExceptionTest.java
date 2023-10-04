package it.pagopa.pn.delivery.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnMandateNotFoundExceptionTest {


    /**
     * Method under test: {@link PnMandateNotFoundException#PnMandateNotFoundException(String)}
     */
    @Test
    void testConstructor2() {
        PnMandateNotFoundException actualPnMandateNotFoundException = new PnMandateNotFoundException("foo");
        assertEquals(404, actualPnMandateNotFoundException.getStatus());
        Problem problem = actualPnMandateNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Mandate not found", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("foo", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_MANDATENOTFOUND, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }

    /**
     * Method under test: {@link PnMandateNotFoundException#PnMandateNotFoundException(String)}
     */
    @Test
    void testConstructor3() {
        PnMandateNotFoundException actualPnMandateNotFoundException = new PnMandateNotFoundException("");
        assertEquals(404, actualPnMandateNotFoundException.getStatus());
        Problem problem = actualPnMandateNotFoundException.getProblem();
        assertNull(problem.getTraceId());
        assertEquals("Mandate not found", problem.getTitle());
        assertEquals("Z", problem.getTimestamp().getOffset().toString());
        assertEquals(404, problem.getStatus().intValue());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        assertEquals("Internal Server Error", problem.getDetail());
        assertEquals("GENERIC_ERROR", problem.getType());
        ProblemError getResult = errors.get(0);
        assertEquals(PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_MANDATENOTFOUND, getResult.getCode());
        assertNull(getResult.getElement());
        assertEquals("none", getResult.getDetail());
    }
}


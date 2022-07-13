package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ProblemError;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.ArrayList;
import java.util.List;

public class HandleWebExchangeBindException {
    private HandleWebExchangeBindException() {}

    public static ResponseEntity<Problem> handleWebExchangeBindException(WebExchangeBindException ex) {
        List<ObjectError> objectErrorList = ex.getAllErrors();
        List<ProblemError> problemErrors = new ArrayList<>();
        objectErrorList.forEach(er -> problemErrors.add(
                new ProblemError().detail(er.getCodes()[0])
        ));
        Problem problem = Problem.builder()
                .title(ex.getReason())
                .type("WebExchangeBindException")
                .status(400)
                .detail(ex.getMessage())
                .errors(problemErrors)
                .build();
        return ResponseEntity.badRequest().body(problem);
    }
}

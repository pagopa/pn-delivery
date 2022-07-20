package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
@Slf4j
public class HandleRuntimeException {
    private HandleRuntimeException() {}

    public static ResponseEntity<Problem> handleRuntimeException(RuntimeException ex) {
        log.error("handleRuntimeException {}", ex.getMessage(), ex);
        List<ProblemError> problemErrorList = Collections.singletonList( ProblemError.builder()
                .code( "" )
                .detail( "" )
                .build() );

        Problem problem = Problem.builder()
                .type( "" )
                .detail( "" )
                .title( "" )
                .status( 500 )
                .errors( problemErrorList )
                .build();

        return ResponseEntity.internalServerError().body( problem );
    }
}

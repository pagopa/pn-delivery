package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.Problem;
import org.springframework.http.ResponseEntity;

public class HandleIllegalArgumentException {

    private HandleIllegalArgumentException() {
        throw new IllegalStateException("Utility class");
    }

    public static ResponseEntity<Problem> handleIllegalArgumentException(IllegalArgumentException ex) {
        Problem problem = Problem.builder()
                .type( "IllegalArgumentException" )
                .detail( ex.getMessage() )
                .status( 400 )
                .build();

        return ResponseEntity.badRequest().body( problem );
    }


}

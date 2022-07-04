package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.ProblemError;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class HandleIdConflict {
    private HandleIdConflict() {}

    public static ResponseEntity<Problem> handleIdConflictException(IdConflictException ex) {
        List<ProblemError> problemErrorList = new ArrayList<>();
        ex.getKeyValueMap().forEach((k,v) -> problemErrorList.add(ProblemError.builder()
                .code( "codice id duplicato" )
                .detail( k+"="+v )
                .build()
        ));

        Problem problem = Problem.builder()
                .type( "IdConflictException" )
                .detail( "I seguenti campi sono già presenti" )
                .title( "Inserimento id duplicati" )
                .status( 400 )
                .errors( problemErrorList )
                .build();

        return ResponseEntity.badRequest().body( problem );
    }
}

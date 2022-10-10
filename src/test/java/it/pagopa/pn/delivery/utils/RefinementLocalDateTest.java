package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

class RefinementLocalDateTest {

    private RefinementLocalDate refinementLocalDate;

    public static final String REFINEMENT_DATE_LEGALE = "2022-10-07T11:01:25.122312Z";
    public static final String EXPECTED_REFINEMENT_DATE_LEGALE = "2022-10-07T23:59:59.999999999+02:00";
    public static final String REFINEMENT_DATE_SOLARE = "2022-12-07T11:01:25.122312Z";
    public static final String EXPECTED_REFINEMENT_DATE_SOLARE = "2022-12-07T23:59:59.999999999+01:00";

    @BeforeEach
    void setup() { refinementLocalDate = new RefinementLocalDate(); }

    @Test
    void checkLocalRefinementDateLegale() {
        // Given
        TimelineElement tle = TimelineElement.builder()
                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE_LEGALE ) )
                .build();

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate( tle );

        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_LEGALE ), result );
    }

    @Test
    void checkLocalRefinementDateSolare() {
        // Given
        TimelineElement tle = TimelineElement.builder()
                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE_SOLARE ) )
                .build();

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate( tle );

        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_SOLARE ), result );
    }

}

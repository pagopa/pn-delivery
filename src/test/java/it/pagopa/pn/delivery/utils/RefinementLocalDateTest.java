package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV24;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

class RefinementLocalDateTest {

    private RefinementLocalDate refinementLocalDate;

    public static final String REFINEMENT_DATE_LEGALE = "2024-10-15T23:07:24Z";
    public static final String INSTANT_DATE_TO_FORMAT = "2022-10-07T11:01:00.000000Z";
    public static final String EXPECTED_REFINEMENT_DATE_LEGALE = "2024-10-16T23:59:59.999999999+02:00";
    public static final String REFINEMENT_DATE_SOLARE = "2022-12-07T23:01:25.122312Z";
    public static final String EXPECTED_REFINEMENT_DATE_SOLARE = "2022-12-08T23:59:59.999999999+01:00";

    @BeforeEach
    void setup() { refinementLocalDate = new RefinementLocalDate(); }

    @Test
    void checkLocalRefinementDateLegale() {
        // Given
        TimelineElementV24 tle = TimelineElementV24.builder()
                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE_LEGALE ) )
                .build();

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate( tle );

        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_LEGALE ), result );
    }

    @Test
    void checkLocalRefinementDateLegale2() {

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate(OffsetDateTime.parse(REFINEMENT_DATE_LEGALE));
        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_LEGALE ), result );
    }

    @Test
    void checkLocalRefinementDateSolare() {
        // Given
        TimelineElementV24 tle = TimelineElementV24.builder()
                .timestamp( OffsetDateTime.parse( REFINEMENT_DATE_SOLARE ) )
                .build();

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate( tle );

        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_SOLARE ), result );
    }

    @Test
    void checkLocalRefinementDateSolare2() {

        // When
        OffsetDateTime result = refinementLocalDate.setLocalRefinementDate(OffsetDateTime.parse(REFINEMENT_DATE_SOLARE));
        // Then
        Assertions.assertEquals( OffsetDateTime.parse( EXPECTED_REFINEMENT_DATE_SOLARE ), result );
    }

    @Test
    void checkFormatDate() {
        String formattedDate = refinementLocalDate.formatInstantToString(Instant.parse( INSTANT_DATE_TO_FORMAT ));

        Assertions.assertEquals( "2022-10-07T11:01:00.000Z", formattedDate );

    }

}

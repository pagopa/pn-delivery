package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Component
public class RefinementLocalDate {

    public static final String ZONE_ID_EUROPE_ROME = "Europe/Rome";

    @NotNull
    public OffsetDateTime setLocalRefinementDate(TimelineElement timelineElement) {
        OffsetDateTime refinementDate;
        OffsetDateTime timestampUtc = timelineElement.getTimestamp();
        // mi sposto all'offest IT
        ZonedDateTime localDateTime = timestampUtc.toLocalDateTime().atZone( ZoneId.of(ZONE_ID_EUROPE_ROME) );
        // mi sposto alle 23:59:59
        refinementDate = OffsetDateTime.of( localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                23, 59, 59, 999999999, ZoneId.of(ZONE_ID_EUROPE_ROME).getRules().getOffset( localDateTime.toInstant() ) );
        return refinementDate;
    }
    @Nullable
    public OffsetDateTime setLocalRefinementDate( OffsetDateTime utcRefinementDate ) {
        OffsetDateTime localRefinementDate = null;
        if ( utcRefinementDate != null ) {
            // mi sposto all'offest IT
            ZonedDateTime localDateTime = utcRefinementDate.toLocalDateTime().atZone( ZoneId.of(ZONE_ID_EUROPE_ROME) );
            // mi sposto alle 23:59:59
            localRefinementDate = OffsetDateTime.of( localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                    23, 59, 59, 999999999, ZoneId.of(ZONE_ID_EUROPE_ROME).getRules().getOffset( localDateTime.toInstant() ) );
        }
        return localRefinementDate;
    }

    @NotNull
    public String formatInstantToString(Instant instantToFormat) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
        return formatter.format(instantToFormat);
    }
}

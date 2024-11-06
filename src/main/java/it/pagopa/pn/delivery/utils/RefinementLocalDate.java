package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementV25;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Component
public class RefinementLocalDate {

    public static final String ZONE_ID_EUROPE_ROME = "Europe/Rome";

    @NotNull
    public OffsetDateTime setLocalRefinementDate(TimelineElementV25 timelineElement) {
        OffsetDateTime timestampUtc = timelineElement.getTimestamp();
        // mi sposto all'offest IT
        OffsetDateTime localDateTime = OffsetDateTime.ofInstant(timestampUtc.toInstant(), ZoneId.of(ZONE_ID_EUROPE_ROME));
        // mi sposto alle 23:59:59
        return localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
    @Nullable
    public OffsetDateTime setLocalRefinementDate( OffsetDateTime utcRefinementDate ) {
        OffsetDateTime localRefinementDate = null;
        if ( utcRefinementDate != null ) {
            // mi sposto all'offest IT
            OffsetDateTime localDateTime = OffsetDateTime.ofInstant(utcRefinementDate.toInstant(), ZoneId.of(ZONE_ID_EUROPE_ROME));
            // mi sposto alle 23:59:59
            localRefinementDate = localDateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        }
        return localRefinementDate;
    }

    @NotNull
    public String formatInstantToString(Instant instantToFormat) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
        return formatter.format(instantToFormat);
    }
}
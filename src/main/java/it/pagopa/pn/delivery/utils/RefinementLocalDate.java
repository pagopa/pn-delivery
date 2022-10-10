package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class RefinementLocalDate {

    @NotNull
    public OffsetDateTime setLocalRefinementDate(TimelineElement timelineElement) {
        OffsetDateTime refinementDate;
        OffsetDateTime timestampUtc = timelineElement.getTimestamp();
        // mi sposto all'offest IT
        ZonedDateTime localDateTime = timestampUtc.toLocalDateTime().atZone( ZoneId.of( "Europe/Rome" ) );
        // mi sposto alle 23:59:59
        refinementDate = OffsetDateTime.of( localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                23, 59, 59, 999999999, ZoneId.of( "Europe/Rome" ).getRules().getOffset( localDateTime.toInstant() ) );
        return refinementDate;
    }
}

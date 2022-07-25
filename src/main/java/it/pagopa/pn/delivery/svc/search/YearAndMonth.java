package it.pagopa.pn.delivery.svc.search;

import lombok.Value;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Value
public class YearAndMonth {

    public static final int MAXIMUM_QUERIED_MONTHS = 120;
    int year;
    int month;

    public YearAndMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static YearAndMonth fromInstant(Instant instant) {
        ZonedDateTime currentMonth = ZonedDateTime.ofInstant( instant, ZoneId.of( "UTC" ) )
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.firstDayOfMonth());

        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();
        return new YearAndMonth( year, month);
    }

    public List<String> generateStringFromThisMonthUntil( YearAndMonth endMonth, String stringPrefix ) {
        List<String> result = new ArrayList<>(MAXIMUM_QUERIED_MONTHS);

        int actualYear = endMonth.year;
        int actualMonth = endMonth.month;

        if( actualYear < this.year || actualYear == this.year && actualMonth < this.month ) {
            throw new IllegalArgumentException("Start (" + this + ") can't be after end (" + endMonth + ")");
        }

        do {
            String oneString = String.format("%s%04d%02d", stringPrefix, actualYear, actualMonth );
            result.add( oneString );

            actualMonth -= 1;
            if( actualMonth == 0 ) {
                actualMonth = 12;
                actualYear -= 1;
            }
        }
        while ( actualYear > this.year || actualYear == this.year && actualMonth >= this.month );

        return result;
    }

}

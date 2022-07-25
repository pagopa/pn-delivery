package it.pagopa.pn.delivery.svc.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

class YearAndMonthTest {

    @ParameterizedTest
    @CsvSource({"2018-11-30T18:35:24.00Z,2018,11"})
    void testFromInstant(String instantString, int expectedYear, int expectedMonth) {
        YearAndMonth month = YearAndMonth.fromInstant( Instant.parse( instantString ) );

        Assertions.assertEquals( expectedYear, month.getYear(), "Year differs" );
        Assertions.assertEquals( expectedMonth, month.getMonth(), "Month differs" );
    }

    @ParameterizedTest
    @CsvSource({"2018-11-30T18:35:24.00Z,2017-11-30T18:35:24.00Z"})
    void testInvalid(String instantFrom, String instantTo) {
        YearAndMonth from = YearAndMonth.fromInstant( Instant.parse( instantFrom ));
        YearAndMonth to = YearAndMonth.fromInstant( Instant.parse( instantTo ));

        Assertions.assertThrows(IllegalArgumentException.class, () -> from.generateStringFromThisMonthUntil( to, "prefix" ));
    }

    @ParameterizedTest
    @CsvSource({
            "2018-11-30T18:35:24.00Z,2018-11-30T18:35:24.00Z,prefix_,prefix_201811",
            "2018-11-30T18:35:24.00Z,2018-12-30T18:35:24.00Z,prefix_,prefix_201812:prefix_201811",
            "2018-11-30T18:35:24.00Z,2019-01-30T18:35:24.00Z,prefix_,prefix_201901:prefix_201812:prefix_201811"
    })
    void testStringListGeneration( String instantFrom, String instantTo, String prefix, String expectedList ) {

        YearAndMonth from = YearAndMonth.fromInstant( Instant.parse( instantFrom ));
        YearAndMonth to = YearAndMonth.fromInstant( Instant.parse( instantTo ));

        List<String> result = from.generateStringFromThisMonthUntil( to, prefix );

        Assertions.assertArrayEquals( expectedList.split(":"), result.toArray( new String[0] ));
    }

}

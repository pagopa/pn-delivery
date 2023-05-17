package it.pagopa.pn.delivery.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class DateUtilsTest {

    @Test
    void createConcatenationTest() {
        String response = DataUtils.createConcatenation("item1","item2");
        Assertions.assertEquals("item1##item2", response);
    }

    @Test
    void extractIUNTest() {
        String response = DataUtils.extractIUN("iun##RecipientId##DelegateId##GroupId");
        Assertions.assertEquals("iun", response);
    }

    @Test
    void extractCreationMonthTest() {
        String response = DataUtils.extractCreationMonth(Instant.parse("2021-09-01T00:00:00Z"));
        Assertions.assertEquals("202109", response);
    }
}

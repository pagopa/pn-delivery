package it.pagopa.pn.delivery.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class DataUtilsTest {

    @Test
    void createConcatenation() {
        Assertions.assertEquals("a##b##c", DataUtils.createConcatenation("a","b","c"));
    }

    @Test
    void extractCreationMonth() {
        Assertions.assertEquals("202101", DataUtils.extractCreationMonth(Instant.parse("2021-01-01T00:00:00Z")));
    }
}

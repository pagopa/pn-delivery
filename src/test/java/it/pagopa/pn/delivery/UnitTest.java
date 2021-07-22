package it.pagopa.pn.delivery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnitTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    void success() {
        log.debug("Do a successful test");
        int actual = 1;
        Assertions.assertEquals(1, actual);
    }


}

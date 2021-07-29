package it.pagopa.pn.delivery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IntegrationTestIT {

    @Test
    void fail() {
        int actual = 2;
        Assertions.assertEquals( 1, actual);
    }
}

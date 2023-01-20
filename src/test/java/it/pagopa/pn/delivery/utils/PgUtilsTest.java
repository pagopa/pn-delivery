package it.pagopa.pn.delivery.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPGAndValuedGroups;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgUtilsTest {

    @Test
    void checkAuthorizationPGAndValuedGroupsTest() {
        String recipientType = "PG";
        List<String> cxGroups = List.of("group1");

        assertTrue(checkAuthorizationPGAndValuedGroups(recipientType, cxGroups));
    }
}
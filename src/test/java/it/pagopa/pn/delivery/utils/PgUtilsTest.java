package it.pagopa.pn.delivery.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.utils.PgUtils.checkAuthorizationPG;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PgUtilsTest {

    @Test
    void checkAuthorizationPGAndValuedGroupsTest() {
        String recipientType = "PG";
        List<String> cxGroups = List.of("group1");

        assertTrue(checkAuthorizationPG(recipientType, cxGroups));
    }

    @Test
    void checkAuthPGEmptyGroups() {
        assertFalse(checkAuthorizationPG("PG", null));
    }

    @Test
    void checkAuthPF() {
        assertFalse(checkAuthorizationPG("PF", null));
    }
}
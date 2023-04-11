package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class InternalIdCleanerTest {

    @Test
    void cleanInternalIdTest() {
        var recipients = List.of(
                new NotificationRecipient().internalId("internal1").denomination("den1"),
                new NotificationRecipient().internalId("internal2").denomination("den2")
        );

        InternalIdCleaner.cleanInternalId(recipients);

        assertThat(recipients.get(0).getInternalId()).isNull();
        assertThat(recipients.get(0).getDenomination()).isEqualTo("den1");
        assertThat(recipients.get(1).getInternalId()).isNull();
        assertThat(recipients.get(1).getDenomination()).isEqualTo("den2");
    }
}

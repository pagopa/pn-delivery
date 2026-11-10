package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.exception.PnDeliveryGroupsUnavailableException;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaGroupServiceTest {

    private static final String SENDER_ID = "senderId";
    private PnExternalRegistriesClientImpl client;
    private PaGroupService service;

    @BeforeEach
    void setup() {
        client = Mockito.mock(PnExternalRegistriesClientImpl.class);
        service = new PaGroupService(client);
    }

    @Test
    void getGroupsForLabelizationShouldReturnGroupsIncludingInactiveOnes() {
        List<PaGroup> groups = List.of(new PaGroup().id("group-id").name("Group name"));
        when(client.getGroups(SENDER_ID, false)).thenReturn(groups);

        assertSame(groups, service.getGroupsForLabelization(SENDER_ID));
        verify(client).getGroups(SENDER_ID, false);
    }

    @Test
    void getGroupsForLabelizationShouldReturnEmptyListWhenGroupsUnavailable() {
        when(client.getGroups(SENDER_ID, false))
                .thenThrow(new PnDeliveryGroupsUnavailableException("Groups unavailable"));

        assertTrue(service.getGroupsForLabelization(SENDER_ID).isEmpty());
        verify(client).getGroups(SENDER_ID, false);
    }

    @Test
    void getGroupsForLabelizationShouldNotSwallowOtherExceptions() {
        IllegalStateException exception = new IllegalStateException("Unexpected error");
        when(client.getGroups(SENDER_ID, false)).thenThrow(exception);

        assertSame(exception, assertThrows(IllegalStateException.class,
                () -> service.getGroupsForLabelization(SENDER_ID)));
    }
}

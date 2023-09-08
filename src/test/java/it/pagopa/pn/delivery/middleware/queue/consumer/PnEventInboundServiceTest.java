package it.pagopa.pn.delivery.middleware.queue.consumer;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PnEventInboundServiceTest {
    /**
     * Method under test: {@link PnEventInboundService#customRouter()}
     */
    @Test
    void testCustomRouter() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R002 Missing observers.
        //   Diffblue Cover was unable to create an assertion.
        //   Add getters for the following fields or make them package-private:
        //     1.this$0

        assertNull((new PnEventInboundService(new EventHandler())).customRouter().functionDefinition(null));
    }
}


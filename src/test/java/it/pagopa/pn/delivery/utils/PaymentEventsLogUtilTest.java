package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventF24;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequestF24;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEventsLogUtilTest {

    public static final String RECIPIENT_TYPE_PF = "PF";
    public static final String PAYMENT_DATE_STRING = "2023-01-17T12:21:00Z";
    public static final String RECIPIENT_TAX_ID = "RSSMRA77E04H501Q";
    public static final String IUN = "IUN";

    private PaymentEventsLogUtil paymentEventsLogUtil;

    @BeforeEach
    void setup() { paymentEventsLogUtil = new PaymentEventsLogUtil(); }

    @Test
    void maskRecipientTaxIdForLog() {
        // Given
        PaymentEventsRequestF24 paymentEventsRequestF24 = PaymentEventsRequestF24.builder()
                .events( List.of( PaymentEventF24.builder()
                                .recipientType( RECIPIENT_TYPE_PF )
                                .recipientTaxId( RECIPIENT_TAX_ID )
                                .iun( IUN )
                                .paymentDate( OffsetDateTime.parse( PAYMENT_DATE_STRING ) )
                        .build() ) )
                .build();

        // When
        String result = paymentEventsLogUtil.maskRecipientTaxIdForLog( paymentEventsRequestF24 );

        // Then
        assertNotNull( result );
        paymentEventsRequestF24.getEvents().forEach(
                e -> assertEquals(e.getRecipientTaxId(),RECIPIENT_TAX_ID)
        );

    }
}

package it.pagopa.pn.delivery.middleware.paymenteventsproducer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsFifoMomProducer;
import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SqsPaymentEventsProducer extends AbstractSqsFifoMomProducer<PnDeliveryPaymentEvent> implements PaymentEventsProducer {
    public SqsPaymentEventsProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper) {
        super(sqsClient, topic, objectMapper, PnDeliveryPaymentEvent.class);
    }
}

package it.pagopa.pn.delivery;

import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.delivery.middleware.NotificationViewedProducer;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class})
public abstract class MockAWSObjectsTest {

    @MockBean(name = "sqsNotificationViewedProducer")
    private NotificationViewedProducer pnDeliveryNotificationViewedEventMomProducer;

    @MockBean
    private PaymentEventsProducer pnDeliveryPaymentEventMomProducer;

}

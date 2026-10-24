package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.db.campaign.CampaignServiceCachedProvider;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer.NotificationViewedProducer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EnableAutoConfiguration()
public abstract class MockAWSObjectsTest {

    @MockitoBean(name = "sqsNotificationViewedProducer")
    private NotificationViewedProducer pnDeliveryNotificationViewedEventMomProducer;

    @MockitoBean
    private PaymentEventsProducer pnDeliveryPaymentEventMomProducer;

    @MockitoBean
    private AsseverationEventsProducer asseverationEventsProducer;

    @MockitoBean
    private InformalNotificationSendPaParameterConsumer informalNotificationSendPaParameterConsumer;

    @MockitoBean
    private CampaignServiceCachedProvider campaignServiceCachedProvider;
}

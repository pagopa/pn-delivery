package it.pagopa.pn.delivery;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import it.pagopa.pn.commons.db.campaign.CampaignServiceCachedProvider;
import it.pagopa.pn.delivery.config.InformalNotificationSendPaParameterConsumer;
import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.middleware.AsseverationEventsProducer;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.middleware.notificationviewedproducer.strategy.producer.NotificationViewedProducer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@EnableAutoConfiguration(exclude= {SqsAutoConfiguration.class})
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

    @MockitoBean
    private SqsAsyncClient sqsAsyncClient;

    @MockitoBean
    private PhysicalAddressLookupParameterConsumer physicalAddressLookupParameterConsumer;
}

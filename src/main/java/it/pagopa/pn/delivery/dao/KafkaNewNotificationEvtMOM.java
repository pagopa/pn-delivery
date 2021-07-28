package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.commons.mom.kafka.GenericKafkaMomConsumer;
import it.pagopa.pn.commons.mom.kafka.GenericKafkaMomProducer;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ConditionalOnProperty( name="pn.mom", havingValue = "kafka")
public class KafkaNewNotificationEvtMOM implements NewNotificationEvtMOM {

    private final String topic;
    private final Consumer<String, NewNotificationEvt> consumer;
    private final KafkaTemplate<String, NewNotificationEvt> kafkaTemplate;

    private GenericKafkaMomConsumer<String, NewNotificationEvt> consumerHelper;
    private GenericKafkaMomProducer<String, NewNotificationEvt> producerHelper;
    private long lastOffset;

    public KafkaNewNotificationEvtMOM(
            String topic,
            Consumer<String, NewNotificationEvt> consumer,
            KafkaTemplate<String, NewNotificationEvt> kafkaTemplate
    ) {
        this.topic = topic;
        this.consumer = consumer;
        this.kafkaTemplate = kafkaTemplate;

        this.producerHelper = new GenericKafkaMomProducer<>( topic, kafkaTemplate );
        this.consumerHelper = new GenericKafkaMomConsumer<>( topic, consumer);
    }

    @Override
    public CompletableFuture<List<NewNotificationEvt>> poll(Duration maxPollTime) {
        return consumerHelper.pollWithOffset( maxPollTime, lastOffset );
    }

    @Override
    public CompletableFuture<Void> push(NewNotificationEvt msg) {
        return producerHelper.pushWithOffset( msg )
                .thenApply( (offset) -> { this.lastOffset = offset; return null; } );
    }
}

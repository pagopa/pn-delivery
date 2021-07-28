package it.pagopa.pn.commons.mom.kafka;

import it.pagopa.pn.commons.mom.MomProducer;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;


public class GenericKafkaMomProducer<K,V> implements MomProducer<V> {

    private final String topic;
    private final KafkaTemplate<K, V> kafkaTemplate;

    public GenericKafkaMomProducer(String topic, KafkaTemplate<K, V> kafkaTemplate) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> push(V msg) {
        return this.pushWithOffset( msg )
                .thenApply( (l) -> null);
    }

    public CompletableFuture<Long> pushWithOffset(V msg) {
        return this.kafkaTemplate
                .send( topic, msg )
                .completable()
                .thenApply( (result) -> result.getRecordMetadata().offset() );
    }
}

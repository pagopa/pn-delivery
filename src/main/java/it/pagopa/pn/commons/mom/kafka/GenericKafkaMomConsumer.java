package it.pagopa.pn.commons.mom.kafka;

import it.pagopa.pn.commons.mom.MomConsumer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class GenericKafkaMomConsumer<K,V> implements MomConsumer<V> {

    private final String topic;
    private final Consumer<K, V> consumer;

    private final TopicPartition topicPartition;

    public GenericKafkaMomConsumer(String topic, Consumer<K, V> kafkaConsumer) {
        this.topic = topic;
        this.consumer = kafkaConsumer;
        this.topicPartition = new TopicPartition( topic, 0 );

        this.consumer.assign(Arrays.asList(topicPartition));
        this.consumer.seek(topicPartition, 0);
    }


    @Override
    public CompletableFuture<List<V>> poll(Duration maxPollTime) {
        ConsumerRecords<K, V> consumerRecords = consumer.poll( maxPollTime );

        List<V> results = new ArrayList<>( consumerRecords.count() );
        for( ConsumerRecord<K, V> record: consumerRecords ) {
            results.add( record.value() );
        }

        return CompletableFuture.completedFuture( results );
    }

    public CompletableFuture<List<V>> pollWithOffset( Duration maxPollTime, long offset ) {
        consumer.seek(topicPartition, offset);
        return this.poll( maxPollTime );
    }

}

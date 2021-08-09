package it.pagopa.pn.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomConsumer;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestKafkaConsumer extends AbstractKafkaMomConsumer<KafkaProducerConsumerTestIT.TestBean> {

    public TestKafkaConsumer(KafkaListenerContainerFactory listenerContainerFactory, ObjectMapper objectMapper) {
        super(listenerContainerFactory, KafkaProducerConsumerTestIT.TOPIC_NAME, KafkaProducerConsumerTestIT.GROUP_NAME, objectMapper, KafkaProducerConsumerTestIT.TestBean.class);
    }
}

package it.pagopa.pn.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomProducer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestKafkaProducer extends AbstractKafkaMomProducer<KafkaProducerConsumerTestIT.TestBean> {

    public TestKafkaProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, KafkaProducerConsumerTestIT.TOPIC_NAME, objectMapper, KafkaProducerConsumerTestIT.TestBean.class);
    }
}

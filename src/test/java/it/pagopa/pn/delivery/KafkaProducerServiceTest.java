package it.pagopa.pn.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import it.pagopa.pn.commons.kafka.KafkaConfigs;
import it.pagopa.pn.delivery.model.message.Message;
import it.pagopa.pn.delivery.model.message.Message.Type;

@SpringBootTest
class KafkaProducerServiceTest {

	private KafkaTemplate<String, Message> kafkaTemplate;
	private Consumer<String, Message> consumer;
	private KafkaConfigs configProperties;
	
	@Autowired
	public KafkaProducerServiceTest(KafkaTemplate<String, Message> kafkaTemplate, Consumer<String, Message> consumer, KafkaConfigs configProperties) {
		this.kafkaTemplate = kafkaTemplate;
		this.consumer = consumer;
		this.configProperties = configProperties;
	}

	@Test
	void test() throws InterruptedException, ExecutionException {		
		String iun = RandomStringUtils.randomAlphanumeric(6);
		Instant now = Instant.now();
		Type type = Type.TYPE1;
		
		//Given
		Message message = new Message();
		message.setIun(iun);
		message.setSentDate(now);
		message.setMessageType(type);
			
		//When
		SendResult<String, Message> sendResult = kafkaTemplate.send(configProperties.getTopic(), message).get();
		
		//Then
		Message received = pollLastMessage(sendResult);
		
		assertThat(received).usingRecursiveComparison().isEqualTo(message);
	}

	private Message pollLastMessage(SendResult<String, Message> sendResult)  {
		TopicPartition topicPartition = new TopicPartition(configProperties.getTopic(), sendResult.getRecordMetadata().partition()); 
		consumer.assign(Arrays.asList(topicPartition)); 
		consumer.seek(topicPartition, sendResult.getRecordMetadata().offset());
		ConsumerRecords<String, Message> consumerRecords = consumer.poll(java.time.Duration.ofMillis(200));
		Message received = consumerRecords.iterator().next().value();
		
		return received;
	}

}

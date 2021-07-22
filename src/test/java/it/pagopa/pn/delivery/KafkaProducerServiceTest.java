package it.pagopa.pn.delivery;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import it.pagopa.pn.delivery.model.message.Message;

@SpringBootTest
@Configuration
public class KafkaProducerServiceTest {

	private KafkaTemplate<String, Message> kafkaTemplate;
	private static final String TOPIC = "topic1";

	@Autowired
	public KafkaProducerServiceTest(KafkaTemplate<String, Message> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Test
	public void test() throws ExecutionException, InterruptedException {
		Message message = new Message();
		message.setIun(RandomStringUtils.randomAlphanumeric(6));
		message.setSentDate(new Date());

		ListenableFuture<SendResult<String, Message>> future = kafkaTemplate.send(TOPIC, message);

		long offset = future.get().getRecordMetadata().offset();
	}

}

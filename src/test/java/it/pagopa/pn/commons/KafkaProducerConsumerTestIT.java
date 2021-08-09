package it.pagopa.pn.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractKafkaMomConsumer;
import it.pagopa.pn.commons.abstractions.MomConsumer;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaProducerConsumerTestIT {

	static final String TOPIC_NAME = "KafkaConsumerTest_testTopic";
	static final String GROUP_NAME = "KafkaConsumerTest_grp1";

	@Autowired
	private TestKafkaProducer producer;

	@Autowired
	private MomConsumer<TestBean> consumer;


	@BeforeAll
	void init() throws InterruptedException {
		Thread.sleep( 1000 );

		// - Necessario per inizializzazione dei topic in caso di test locali
		consumer.poll( Duration.ofSeconds(1) );
	}

	@Test
	public void test() throws InterruptedException {

		// - Given
		TestBean bean = TestBean.builder()
				.name("Donald")
				.surname("Duck")
				.age( randomAge() )
				.build();

		// - When
		producer.push( bean );
		List<TestBean> receivedBeans = consumer.poll( Duration.ofSeconds(10) );

		// - Then
		Assertions.assertTrue( receivedBeans.size() > 0, "Ricevuto almeno un messaggio");
		TestBean lastReceived = receivedBeans.get( receivedBeans.size() - 1 );
		Assertions.assertEquals( bean, lastReceived, "Sended and received messages differs");
	}

	private Integer randomAge() {
		return new Random().nextInt( 100);
	}



	@Value
	@Builder( toBuilder = true)
	@EqualsAndHashCode()
	public static class TestBean {

		private String name;
		private String surname;
		private Integer age;

	}

}

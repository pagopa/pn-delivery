package it.pagopa.pn.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import it.pagopa.pn.commons.mom.MomConsumer;
import it.pagopa.pn.commons.mom.MomProducer;
import it.pagopa.pn.delivery.dao.KafkaNewNotificationEvtMOM;
import it.pagopa.pn.delivery.dao.NewNotificationEvtMOM;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import it.pagopa.pn.commons.mom.kafka.KafkaConfigs;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt.Type;

@SpringBootTest()
class KafkaProducerServiceTestIT {

	@Autowired
	private NewNotificationEvtMOM mom;

	@Test
	void test() throws ExecutionException, InterruptedException {
		String iun = RandomStringUtils.randomAlphanumeric(6);
		Instant now = Instant.now();
		Type type = Type.TYPE1;
		
		//Given
		NewNotificationEvt message = NewNotificationEvt.builder()
				.iun( iun )
				.sentDate( now )
				.messageType( type )
				.build();

		//When
		mom.push( message ).get();
		NewNotificationEvt received = mom.poll( Duration.ofMillis(400) ).get().get( 0 );

		//Then
		assertThat(received).usingRecursiveComparison().isEqualTo(message);
	}

}

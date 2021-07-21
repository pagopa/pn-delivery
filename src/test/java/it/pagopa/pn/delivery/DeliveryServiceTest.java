package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.aws.AwsConfigs;
import it.pagopa.pn.commons.aws.AwsServicesClientsConfig;
import it.pagopa.pn.delivery.dao.DynamoDeliveryDAO;
import it.pagopa.pn.delivery.model.notification.Notification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class DeliveryServiceTest {

    private DynamoDeliveryDAO dao;

    @Autowired
    private AwsServicesClientsConfig cfg;

    @BeforeEach
    public void initDao() {
        Assertions.assertNotNull( cfg );
        this.dao = new DynamoDeliveryDAO( cfg.dynamoDbEnhancedAsyncClient() );
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        Notification n = new Notification();
        n.setPaNotificationId("paNot1");

        n.setIun("iun1");
        CompletableFuture cf = dao.addNotification( n );
        cf.get();
                //.exceptionally( t -> {t.printStackTrace(); return null; })
                //.get();

    }


}

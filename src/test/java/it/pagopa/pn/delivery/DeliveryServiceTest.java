package it.pagopa.pn.delivery;

import it.pagopa.pn.delivery.dao.DeliveryDAO;
import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootTest
public class DeliveryServiceTest {


    private final NotificationRepository notificationRepository;

    @Autowired
    public DeliveryServiceTest( NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Test
    public void testScrittura()  {
        for (int i=1; i<5 ; i++){
            Notification notification = new Notification();
            notification.setPaNotificationId("paNot" + i);

            notification.setIun("iun" + i);
            notificationRepository.save(notification);
        }

    }

    @Test
    public void testLettura() {
        List<Notification> listaNotifiche = (List<Notification>) notificationRepository.findAll();
        System.err.println(listaNotifiche);
    }

}

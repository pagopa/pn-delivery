package it.pagopa.pn.delivery;


import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.CassandraInvalidQueryException;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {})
public class DeliveryServiceTestIT {

    //cercare come fare delle properties specifiche per il test
    private final NotificationRepository notificationRepository;

    @Autowired
    public DeliveryServiceTestIT(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Test
    public void testCassandraLetturaScrittura() {

        //
        //Given
        String id1 = "iun1";
        Notification notification1 = Notification.builder().build();
        notification1.setPaNotificationId("paNot1");
        notification1.setIun(id1);
        notification1.setTimeline(Arrays.asList(TimelineElement.builder().timestamp(Instant.now().truncatedTo(ChronoUnit.MILLIS)).build()));

        String id2 = "iun2";
        Notification notification2 = Notification.builder().build();
        notification2.setPaNotificationId("paNot2");
        notification2.setIun(id2);
//usare il builder al posto dei setter\
        //
        //When
        notificationRepository.deleteAll();
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        //
        // Then
        // RiletturaById
        Optional<Notification> notificationRead1 = notificationRepository.findById(id1);
        Optional<Notification> notificationRead2 = notificationRepository.findById(id2);

        // assertTrue(notificationRead1.isPresent());
        if (notificationRead1.isPresent()) {
            assertEquals(notification1,notificationRead1.get());
        }
/*
        if (notificationRead2.isPresent()) {
            assertEquals(notification2,notificationRead2.get());
        }
*/
        // findAll deve dare un risultato ccon due elementi diversi
        List<Notification> listaNotifiche = (List<Notification>) notificationRepository.findAll();
        assertEquals(2,listaNotifiche.size());
        assertNotEquals(listaNotifiche.get(0),listaNotifiche.get(1));


    }

    @Test
    public void testEmptyNotification(){
        // Given
        //
        Notification notification = Notification.builder().build();

        //When
        //
        Executable saveNotification = () -> {
                notificationRepository.save(notification);
        };

        //Then
        //
        assertThrows(CassandraInvalidQueryException.class, saveNotification);

    }

    @Test
    public void testNotificationIunOnly(){
        // Given
        //
        String iun = "iun1";
        Notification notification = Notification.builder().iun(iun).build();

        //When
        //
        notificationRepository.save(notification);

        //Then
        //
        Optional<Notification> notificationRead = notificationRepository.findById(iun);
        assertTrue(notificationRead.isPresent());
        assertEquals(notification,notificationRead.get());


    }

    //Fare testPiccoli per gli errori
    //istante troncato e non troncato verifica che l' istante sia diverso e dopo troncati in millisecondi originale
    //Test grande in cui inserisco dati


 /*
 * data una notifica quando scrivi su cassandra verifico che la find all sia un insieme di due elementi che sono quelli scritti
 * due istanze diverse (assert) puntatori diversi  equals sui campi
 * entità test notification e scriverla su una
 * cassandraTestNotification entity di test per containerizzare il test e renderlo indipendente
 */
}

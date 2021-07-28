package it.pagopa.pn.delivery;


import it.pagopa.pn.delivery.model.notification.*;
import it.pagopa.pn.delivery.model.notification.address.DigitalAddress;
import it.pagopa.pn.delivery.model.notification.address.PhysicalAddress;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatus;
import it.pagopa.pn.delivery.model.notification.status.NotificationStatusHistoryElement;
import it.pagopa.pn.delivery.model.notification.timeline.TimelineElement;
import it.pagopa.pn.delivery.dao.CassandraNotificationRepository;
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
public class CassandraNotificationRepositoryTestIT {

    //cercare come fare delle properties specifiche per il test
    private final CassandraNotificationRepository cassandraNotificationRepository;
    private static final String IUN1 = CassandraNotificationRepositoryTestIT.class.getName() + "_iun_1";
    private static final String IUN2 = CassandraNotificationRepositoryTestIT.class.getName() + "_iun_2";

    @Autowired
    public CassandraNotificationRepositoryTestIT(CassandraNotificationRepository cassandraNotificationRepository) {
        this.cassandraNotificationRepository = cassandraNotificationRepository;
    }

    @Test
    public void testCassandraLetturaScrittura() {

        //
        //Given
        String id1 = IUN1;
        Notification notification1 = Notification.builder()
                .iun(id1)
                .paNotificationId("paNot1")
                .cancelledIun("cancellediun")
                .subject("subject 1")
                .cancelledByIun("cancelledByIun1")
                .notificationStatus(NotificationStatus.DELIVERED)
                .notificationStatusHistory(Arrays.asList(NotificationStatusHistoryElement.builder()
                        .status(NotificationStatus.PAID)
                        .activeFrom(Instant.now().truncatedTo(ChronoUnit.MILLIS))
                        .build()))
                .recipients(Arrays.asList(NotificationRecipient.builder()
                        .digitalDomicile(DigitalAddress.builder()
                                .address("via Roma 122")
                                .type(DigitalAddress.Type.PEC).build())
                        .physicalAddress(new PhysicalAddress())
                        .fc("fc1")
                        .build()))
                .payment(NotificationPaymentInfo.builder()
                        .iuv("iuv1")
                        .f24(NotificationPaymentInfo.F24.builder()
                                .analog(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .digital(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .flatRate(NotificationAttachment.builder()
                                        .body("body1")
                                        .contentType("contentType1")
                                        .build())
                                .build())
                        .notificationFeePolicy(NotificationPaymentInfo.FeePolicies.DELIVERY_MODE)
                        .build())
                .documents(Arrays.asList(NotificationAttachment.builder()
                        .body("body1")
                        .contentType("contentType1")
                        .build()))
                .sender(NotificationSender.builder()
                        .paId("paId1")
                        .paName("paName1")
                        .build())
                .timeline(Arrays.asList(TimelineElement.builder().timestamp(Instant.now().truncatedTo(ChronoUnit.MILLIS)).build()))
                .build();


        String id2 = IUN2;
        Notification notification2 = Notification.builder()  //liste vuote
                .iun(id2)
                .paNotificationId("paNot2")
                .build();

        //
        //When
        cassandraNotificationRepository.deleteAll();
        cassandraNotificationRepository.save(notification1);
        cassandraNotificationRepository.save(notification2);
        //
        // Then
        // RiletturaById
        Optional<Notification> notificationRead1 = cassandraNotificationRepository.findById(id1);
        Optional<Notification> notificationRead2 = cassandraNotificationRepository.findById(id2);

        assertTrue(notificationRead1.isPresent());
        if (notificationRead1.isPresent()) {
            assertEquals(notification1,notificationRead1.get());
        }

        assertTrue(notificationRead2.isPresent());
        if (notificationRead2.isPresent()) {
            assertEquals(notification2,notificationRead2.get());
        }

        // findAll deve dare un risultato ccon due elementi diversi
        List<Notification> listaNotifiche = (List<Notification>) cassandraNotificationRepository.findAll();
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
                cassandraNotificationRepository.save(notification);
        };

        //Then
        //
        assertThrows(CassandraInvalidQueryException.class, saveNotification);

    }

    @Test
    public void testNotificationIunOnly(){
        // Given
        //
        String iun = IUN1;
        Notification notification = Notification.builder().iun(iun).build();

        //When
        //
        cassandraNotificationRepository.deleteById(iun); // elimino quello con lo stesso id se gia presente
        cassandraNotificationRepository.save(notification);

        //Then
        //
        Optional<Notification> notificationRead = cassandraNotificationRepository.findById(iun);
        assertTrue(notificationRead.isPresent());
        assertEquals(notification,notificationRead.get());

    }

    @Test
    public void testListNotNull(){
        // Given
        //
        String iun = IUN1;
        Notification notification = Notification.builder().iun(iun).build();

        //When
        //
        cassandraNotificationRepository.deleteById(iun); // elimino quello con lo stesso id se gia presente
        cassandraNotificationRepository.save(notification);

        //Then
        //
        Optional<Notification> notificationRead = cassandraNotificationRepository.findById(iun);
        assertTrue(notificationRead.isPresent());
        assertTrue(notificationRead.get().getTimeline() == null);

    }

    @Test
    public void testIstantTruncation(){
        /*
        * cassandra trasforma il tipo Istant di java in un suo tipo Timestamp che è troncato ai millisecondi. */
        //
        //Given
        String id1 = IUN1;
        Notification notification1 = Notification.builder() //troncato prima della memorizzazione
                .iun(id1)
                .paNotificationId("paNot1")
                .timeline(Arrays.asList(TimelineElement.builder().timestamp(Instant.now().truncatedTo(ChronoUnit.MILLIS)).build()))
                .build();

        String id2 = IUN2;
        Notification notification2 = Notification.builder()  //liste vuote
                .iun(id2)
                .paNotificationId("paNot2")
                .timeline(Arrays.asList(TimelineElement.builder().timestamp(Instant.now()).build()))
                .build();

        //
        //When
        cassandraNotificationRepository.deleteAll();
        cassandraNotificationRepository.save(notification1);
        cassandraNotificationRepository.save(notification2);
        //
        // Then
        // RiletturaById
        Optional<Notification> notificationRead1 = cassandraNotificationRepository.findById(id1);
        Optional<Notification> notificationRead2 = cassandraNotificationRepository.findById(id2);

        assertTrue(notificationRead1.isPresent());
        if (notificationRead1.isPresent()) {
            assertEquals(notification1,notificationRead1.get());
        }

        assertTrue(notificationRead2.isPresent());
        if (notificationRead2.isPresent()) {
            assertNotEquals(notification2,notificationRead2.get()); // la seconda viene troncata durante la memorizzazione
        }

    }


}

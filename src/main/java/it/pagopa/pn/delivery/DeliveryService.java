package it.pagopa.pn.delivery;

import it.pagopa.pn.delivery.dao.DeliveryDAO;
import it.pagopa.pn.delivery.dao.NewNotificationEvtMOM;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt;
import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.NotificationAck;
import it.pagopa.pn.delivery.model.notification.NotificationSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DeliveryService {

    private final Clock clock;

    private final DeliveryDAO deliveryDao;
    private final NewNotificationEvtMOM mom;

    public DeliveryService(DeliveryDAO deliveryDao, NewNotificationEvtMOM mom, Clock clock) {
        this.clock = clock;
        this.deliveryDao = deliveryDao;
        this.mom = mom;
    }

    public CompletableFuture<NotificationAck> receiveNotification(String paId, Notification notification) {

        if( ! checkPaNotificationId( paId ) ) {
            throw new IllegalArgumentException(); //FIXME gestione messaggistica
        }
        notification.setSender( NotificationSender.builder().build() );
        notification.getSender().setPaId( paId.trim() );

        String paNotificationId = notification.getPaNotificationId();
        if( ! checkPaNotificationId( paNotificationId ) ) {
            throw new IllegalArgumentException(); //FIXME gestione messaggistica
        }

        // - verificare presenza almeno un destinatario

        // - Verificare sha256

        String iun = generateIun();
        notification.setIun( iun );

        // - save
        return mom.push(
                NewNotificationEvt.builder()
                        .iun( iun )
                        .sentDate( clock.instant() )
                        .build()
            ).thenCompose( (v1) ->
                deliveryDao.addNotification( notification )
                        .thenApply( (v2) -> NotificationAck.builder().iun( iun ).paNotificationId( paNotificationId ).build() )
        );
    }

    private String generateIun() {
        String uuid = UUID.randomUUID().toString();

        Instant now = Instant.now( clock );
        OffsetDateTime nowUtc = now.atOffset( ZoneOffset.UTC );
        int year = nowUtc.get( ChronoField.YEAR_OF_ERA);
        int month = nowUtc.get( ChronoField.MONTH_OF_YEAR);
        return String.format("%04d%02d-%s", year, month, uuid);
    }

    private boolean checkPaNotificationId(String paNotificationId) {
        return StringUtils.isNotBlank( paNotificationId );
    }
}

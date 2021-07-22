package it.pagopa.pn.delivery;

import it.pagopa.pn.delivery.dao.DeliveryDAO;
import it.pagopa.pn.delivery.model.notification.Notification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DeliveryService {

    @Autowired
    private Clock clock;

    @Autowired
    private DeliveryDAO deliveryDao;
    
    public CompletableFuture<Void> receiveNotification(String paId, Notification notification) {

        if( ! checkPaNotificationId( paId ) ) {
            throw new IllegalArgumentException(); //FIXME gestione messaggistica
        }
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
        return deliveryDao.addNotification( notification );
    }

    private String generateIun() {
        String uuid = UUID.randomUUID().toString();

        Instant now = Instant.now( clock );
        OffsetDateTime nowUtc = now.atOffset( ZoneOffset.UTC );
        int year = nowUtc.get( ChronoField.YEAR_OF_ERA);
        int month = nowUtc.get( ChronoField.MONTH_OF_YEAR);
        return year + month + '-' + uuid;
    }

    private boolean checkPaNotificationId(String paNotificationId) {
        return StringUtils.isNotBlank( paNotificationId );
    }
}

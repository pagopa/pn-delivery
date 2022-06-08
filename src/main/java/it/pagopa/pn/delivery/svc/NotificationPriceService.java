package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class NotificationPriceService {
    private final NotificationEntityDao entityDao;
    private final NotificationRetrieverService retrieverService;
    private final PnDeliveryConfigs cfg;

    public NotificationPriceService(NotificationEntityDao entityDao, NotificationRetrieverService retrieverService, PnDeliveryConfigs cfg) {
        this.entityDao = entityDao;
        this.retrieverService = retrieverService;
        this.cfg = cfg;
    }

    public NotificationPriceResponse getNotificationPrice(String paTaxId, String noticeNumber) {
        // richiesta al dao per recuperare la notifica dato paTaxId e noticeNumber
        InternalNotification notification = null;
        Date effectiveDate = null;
        String amount = null;
        Optional<List<InternalNotification>> optionalInternalNotifications = entityDao.getNotificationByPaymentInfo( paTaxId, noticeNumber );
        if (optionalInternalNotifications.isPresent()) {
            if (optionalInternalNotifications.get().isEmpty() || optionalInternalNotifications.get().size() > 1) {
                log.error( "Unable to find any notification by paTaxId={} noticeNumber={}", paTaxId, noticeNumber );
                throw new PnNotFoundException( String.format( "Unable to find any notification by paTaxId=%s noticeNumber=%s", paTaxId, noticeNumber ) );
            } else {
                notification = optionalInternalNotifications.get().get( 0 );
                // recupero degli elementi di timeline per prendere la data di effective date
                notification = retrieverService.enrichWithTimelineAndStatusHistory( notification.getIun(), notification );
                Optional<TimelineElement> timelineElement = notification.getTimeline()
                        .stream()
                        .filter( tle -> tle.getCategory().equals( TimelineElementCategory.REFINEMENT ) )
                        .findFirst();
                if (timelineElement.isPresent()){
                     effectiveDate = timelineElement.get().getTimestamp();
                     // calcolo costo della notifica
                     amount = computeAmount( notification, noticeNumber );
                } else {
                    log.error( "Unable to find timeline element category={} iun={}", TimelineElementCategory.REFINEMENT, notification.getIun() );
                    throw new PnNotFoundException( String.format( "Unable to find timeline element category=%s iun=%s", TimelineElementCategory.REFINEMENT, notification.getIun() ) );
                }
            }
        } else {
            log.info( "No notification by paTaxId={} noticeNumber={}", paTaxId, noticeNumber );
            throw new PnNotFoundException( String.format( "No notification by paTaxId=%s noticeNumber=%s", paTaxId, noticeNumber ) );
        }

        // creazione dto response
        return NotificationPriceResponse.builder()
                .amount( amount )
                .iun( notification.getIun() )
                .effectiveDate( effectiveDate )
                .build();
    }

    private String computeAmount(InternalNotification notification, String noticeNumber) {
        // recupero destinatario tramite noticeNumber
        // TODO forse non è necessario recuperare l'oggetto recipient basta l'indice
        Optional<NotificationRecipient> recipient = notification.getRecipients()
                .stream()
                .filter( r -> r.getPayment().getNoticeCode().equals( noticeNumber ))
                .findFirst();
        if (!recipient.isPresent()) {
            log.error( "Unable to find any notification recipient for noticeNumber={}", noticeNumber );
            throw new PnNotFoundException( String.format( "Unable to find any notification recipient for noticeNumber=%s", noticeNumber ) );
        }
        int recipientIndex = notification.getRecipients().indexOf( recipient.get() );
        PnDeliveryConfigs.Costs costs = cfg.getCosts();
        // costo di notifica per destinatrio
        long notificationCost = Long.parseLong( costs.getNotification() );

        for ( TimelineElement tle : notification.getTimeline() ) {
            if (tle.getCategory() == TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER && recipientIndex == tle.getDetails().getRecIndex()) {
                notificationCost += computeSimpleRegisteredLetterCost(tle);
            }
        }
        return Long.toString(notificationCost);
    }

    private Long computeSimpleRegisteredLetterCost(TimelineElement tle) {
        long simpleRegisteredLetterCost;
        String foreignState = tle.getDetails().getPhysicalAddress().getForeignState();
        if (foreignState != null && foreignState.equals( "Italia" )) {
            simpleRegisteredLetterCost = Long.parseLong( cfg.getCosts().getRaccomandataIta() );
        } else {
            // TODO recupero della zona in base al codice foreignState
            simpleRegisteredLetterCost = Long.parseLong( cfg.getCosts().getRaccomandataEstZona1() );
        }
        return simpleRegisteredLetterCost;
    }
}

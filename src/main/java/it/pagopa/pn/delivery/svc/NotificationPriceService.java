package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationCostResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationPriceResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationCostEntityDao;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.InternalNotificationCost;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Slf4j
public class NotificationPriceService {
    private final NotificationCostEntityDao notificationCostEntityDao;
    private final NotificationDao notificationDao;
    private final NotificationRetrieverService retrieverService;
    private final PnDeliveryConfigs cfg;

    public NotificationPriceService(NotificationCostEntityDao notificationCostEntityDao, NotificationDao notificationDao, NotificationRetrieverService retrieverService, PnDeliveryConfigs cfg) {
        this.notificationCostEntityDao = notificationCostEntityDao;
        this.notificationDao = notificationDao;
        this.retrieverService = retrieverService;
        this.cfg = cfg;
    }

    public NotificationPriceResponse getNotificationPrice(String paTaxId, String noticeCode) {
        // richiesta al dao per recuperare la notifica dato paTaxId e noticeCode
        OffsetDateTime effectiveDate = null;
        String amount = null;
        log.info( "Get notification cost for paTaxId={} noticeCode={}", paTaxId, noticeCode );
        Optional<InternalNotificationCost> optionalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo( paTaxId, noticeCode );
        if (optionalNotificationCost.isPresent()) {
            log.info( "Get notification with iun={}", optionalNotificationCost.get().getIun() );
                Optional<InternalNotification> optionalNotification = notificationDao.getNotificationByIun( optionalNotificationCost.get().getIun() );
                if (optionalNotification.isPresent()) {
                    // recupero degli elementi di timeline per prendere la data di effective date
                    log.info( "Get notification timeline for iun={}", optionalNotificationCost.get().getIun() );
                    InternalNotification notification = retrieverService.enrichWithTimelineAndStatusHistory( optionalNotificationCost.get().getIun(), optionalNotification.get() );
                    Optional<TimelineElement> timelineElement = notification.getTimeline()
                            .stream()
                            .filter( tle -> TimelineElementCategory.REFINEMENT.equals(tle.getCategory()) || TimelineElementCategory.NOTIFICATION_VIEWED.equals( tle.getCategory() ))
                            .findFirst();
                    if (timelineElement.isPresent()){
                        effectiveDate = timelineElement.get().getTimestamp();
                    }
                    // calcolo costo della notifica
                    amount = computeAmount( optionalNotificationCost.get().getRecipientIdx(), notification );
                } else {
                    log.error( "Unable to find notification for iun={}", optionalNotificationCost.get().getIun() );
                    throw new PnNotFoundException( String.format("Unable to find notification for iun=%s", optionalNotificationCost.get().getIun() ));
                }

        } else {
            log.info( "No notification by paTaxId={} noticeCode={}", paTaxId, noticeCode );
            throw new PnNotFoundException( String.format( "No notification by paTaxId=%s noticeCode=%s", paTaxId, noticeCode ) );
        }

        // creazione dto response
        return NotificationPriceResponse.builder()
                .amount( amount )
                .iun( optionalNotificationCost.get().getIun() )
                .effectiveDate( effectiveDate )
                .build();
    }

    private String computeAmount(int recipientIdx, InternalNotification notification) {
        log.info( "Compute notification cost amount for recipientIdx={} iun={}", recipientIdx, notification.getIun() );
        long notificationCost;
        switch ( notification.getNotificationFeePolicy() ) {
            case FLAT_RATE: {
                log.info( "Notification cost amount for FLATE_RATE" );
                notificationCost = 0L;
            } break;
            case DELIVERY_MODE: {
                log.info( "Compute notification cost amount for DELIVERY_MODE" );
                PnDeliveryConfigs.Costs costs = cfg.getCosts();
                // costo di notifica per destinatrio
                notificationCost = Long.parseLong( costs.getNotification() );
                for ( TimelineElement tle : notification.getTimeline() ) {
                    if (tle.getCategory() == TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER && recipientIdx == tle.getDetails().getRecIndex()) {
                        notificationCost += computeSimpleRegisteredLetterCost(tle);
                    }
                } break;
            }
            default: throw new UnsupportedOperationException();
        }
        return Long.toString(notificationCost);
    }

    private Long computeSimpleRegisteredLetterCost(TimelineElement tle) {
        log.info( "Compute simple register letter cost for timelineElementId={}", tle.getElementId() );
        long simpleRegisteredLetterCost;
        String foreignState = tle.getDetails().getPhysicalAddress().getForeignState();
        log.debug( "ForeignState={}", foreignState );
        if (foreignState == null || foreignState.equals( "Italia" )) {
            simpleRegisteredLetterCost = Long.parseLong( cfg.getCosts().getRaccomandataIta() );
        } else {
            // TODO nella PN-1567 recupero della zona in base al codice foreignState
            simpleRegisteredLetterCost = Long.parseLong( cfg.getCosts().getRaccomandataEstZona1() );
        }
        return simpleRegisteredLetterCost;
    }

    public NotificationCostResponse getNotificationCost(String paTaxId, String noticeCode) {
        String iun;
        int recipientIdx;
        log.info( "Get notification cost info for paTaxId={} noticeCode={}", paTaxId, noticeCode );
        Optional<InternalNotificationCost> optionalNotificationCost = notificationCostEntityDao.getNotificationByPaymentInfo( paTaxId, noticeCode );
        if (optionalNotificationCost.isPresent()) {
            iun = optionalNotificationCost.get().getIun();
            recipientIdx = optionalNotificationCost.get().getRecipientIdx();
        } else {
            log.info( "No notification cost info by paTaxId={} noticeCode={}", paTaxId, noticeCode );
            throw new PnNotFoundException( String.format( "No notification cost info by paTaxId=%s noticeCode=%s", paTaxId, noticeCode ) );
        }

        return NotificationCostResponse.builder()
                .iun( iun )
                .recipientIdx( recipientIdx )
                .build();
    }
}

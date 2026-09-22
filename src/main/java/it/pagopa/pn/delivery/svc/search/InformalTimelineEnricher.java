package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusHistoryElementV1;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalTimelineElementV1;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.utils.RefinementLocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.delivery.utils.NotificationUtils.removeDocuments;

@Service
@RequiredArgsConstructor
@Slf4j
public class InformalTimelineEnricher implements TimelineEnricher<InformalNotificationDetail> {

    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final ModelMapper modelMapper;
    private final PnDeliveryConfigs cfg;
    private final Clock clock;
    private final RefinementLocalDate refinementLocalDateUtils;

    @Override
    public void enrichNotificationDetail(InformalNotificationDetail informalNotificationDetail, boolean requestBySender) {
        InternalNotification notification = informalNotificationDetail.getNotification();
        String iun = notification.getIun();
        enrichWithTimelineAndStatusHistory(iun, informalNotificationDetail);
        OffsetDateTime acceptanceDate = findAcceptanceDate(informalNotificationDetail.getTimeline(), notification.getIun());
        checkDocumentsAvailability(informalNotificationDetail, acceptanceDate);
    }

    private void enrichWithTimelineAndStatusHistory(String iun, InformalNotificationDetail informalNotificationDetail) {
        log.debug("Retrieve timeline for iun={}", iun);
        int numberOfRecipients = informalNotificationDetail.getNotification().getRecipients().size();
        OffsetDateTime createdAt = informalNotificationDetail.getNotification().getSentAt();

        InformalNotificationHistoryResponse informalNotificationHistory = pnDeliveryPushClient.getInformalNotificationHistory(
                iun,
                numberOfRecipients, createdAt);
        // la lista arriva già ordinata correttamente
        var timelineList = informalNotificationHistory.getTimeline();

        log.debug("Retrieve status history for informalNotificationDetail created at={}", createdAt);

        List<it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusHistoryElementV1> statusHistory = informalNotificationHistory.getInformalNotificationStatusHistory();

        enrichInformalNotification(informalNotificationDetail,
                Objects.requireNonNull(timelineList),
                Objects.requireNonNull(statusHistory),
                Objects.requireNonNull(informalNotificationHistory));
    }

    private void enrichInformalNotification(InformalNotificationDetail informalNotificationDetail,
                                            List<InformalTimelineElementV1> timelineList,
                                            List<InformalNotificationStatusHistoryElementV1> statusHistory,
                                            InformalNotificationHistoryResponse informalNotificationHistory) {

        informalNotificationDetail.setTimeline(timelineList.stream()
                .map(timelineElement -> modelMapper.map(timelineElement, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1.class))
                .toList());

        informalNotificationDetail.setNotificationStatusHistory(statusHistory.stream()
                .map(el -> modelMapper.map(el, it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusHistoryElementV1.class))
                .toList());

        informalNotificationDetail.setNotificationStatus(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1
                .fromValue(Objects.requireNonNull(informalNotificationHistory.getInformalNotificationStatus()).getValue()));
    }

    private OffsetDateTime findAcceptanceDate(List<it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1> timeline, String iun) {
        log.debug("Find acceptance date iun={}", iun);
        OffsetDateTime acceptanceDate = null;
        // cerco elemento timeline con category request_accepted
        Optional<OffsetDateTime> optionalDate = timeline
                .stream()
                .filter(tle -> it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementCategoryV1.REQUEST_ACCEPTED.equals(tle.getCategory()))
                .map(it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalTimelineElementV1::getIngestionTimestamp)
                .filter(Objects::nonNull)
                .findFirst();
        // se trovo la data di accettazione della notifica
        if (optionalDate.isPresent()) {
            acceptanceDate = refinementLocalDateUtils.setLocalRefinementDate(optionalDate.get());
        } else {
            log.debug("Notification iun={} not accepted", iun);
        }
        return acceptanceDate;
    }

    private void checkDocumentsAvailability(InformalNotificationDetail informalNotificationDetail, OffsetDateTime acceptanceDate) {
        InternalNotification notification = informalNotificationDetail.getNotification();
        log.debug("Check if documents are available for iun={}", notification.getIun());
        
        if (!hasDocumentsPresent(notification)) {
            log.debug("Documents not present for iun={}", notification.getIun());
            notification.setDocumentsAvailable(null);
            return;
        }
        
        if (acceptanceDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(
                    acceptanceDate.toInstant().truncatedTo(ChronoUnit.DAYS),
                    clock.instant().truncatedTo(ChronoUnit.DAYS)
            );
            if (daysBetween > Long.parseLong(cfg.getInformalMaxDocumentsAvailableDays())) {
                log.debug("Documents expired for iun={} from={}", notification.getIun(), acceptanceDate);
                removeDocuments(notification);
                return;
            }
        }
        
        notification.setDocumentsAvailable(true);
    }
    private static boolean hasDocumentsPresent(InternalNotification notification) {
        return Objects.nonNull(notification.getDocuments()) && !notification.getDocuments().isEmpty();
    }
}

package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationHistoryResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusHistoryElementV1;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.InformalTimelineElementV1;
import it.pagopa.pn.delivery.models.InformalNotificationDetail;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class InformalTimelineEnricher implements TimelineEnricher<InformalNotificationDetail> {

    private final PnDeliveryPushClientImpl pnDeliveryPushClient;
    private final ModelMapper modelMapper;

    @Override
    public void enrichNotificationDetail(InformalNotificationDetail informalNotificationDetail, boolean requestBySender) {
        InternalNotification notification = informalNotificationDetail.getNotification();
        String iun = notification.getIun();
        enrichWithTimelineAndStatusHistory(iun, informalNotificationDetail);
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

        List<InformalNotificationStatusHistoryElementV1> statusHistory = informalNotificationHistory.getInformalNotificationStatusHistory();

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
                .map(timelineElement -> modelMapper.map(timelineElement, InformalTimelineElementV1.class))
                .toList());

        informalNotificationDetail.setNotificationStatusHistory(statusHistory.stream()
                .map(el -> modelMapper.map(el, InformalNotificationStatusHistoryElementV1.class))
                .toList());

        informalNotificationDetail.setNotificationStatus(InformalNotificationStatusV1
                .fromValue(Objects.requireNonNull(informalNotificationHistory.getInformalNotificationStatus()).getValue()));
    }
}

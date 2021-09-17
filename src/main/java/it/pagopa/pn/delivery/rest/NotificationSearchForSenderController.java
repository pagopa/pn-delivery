package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodSearchSentNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.sendersearch.NotificationSearchForSenderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class NotificationSearchForSenderController implements PnDeliveryRestApi_methodSearchSentNotification {

    private final NotificationSearchForSenderService svc;

    public NotificationSearchForSenderController(NotificationSearchForSenderService svc) {
        this.svc = svc;
    }

    @GetMapping(PnDeliveryRestConstants.SENDER_NOTIFICATIONS_PATH)
    public List<NotificationSearchRow> searchSentNotification(
            @RequestParam(name = "senderId") String senderId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "recipientId", required = false) String recipientId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp
    ) {
        return svc.searchSentNotification(senderId, startDate, endDate, recipientId, status, subjectRegExp);
    }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodSearchReceivedNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodSearchSentNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.recipientsearch.NotificationSearchForRecipientService;
import it.pagopa.pn.delivery.svc.sendersearch.NotificationSearchForSenderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
public class NotificationSearchForRecipientController implements PnDeliveryRestApi_methodSearchReceivedNotification {

    private final NotificationSearchForRecipientService svc;

    public NotificationSearchForRecipientController(NotificationSearchForRecipientService svc) {
        this.svc = svc;
    }

    @GetMapping(PnDeliveryRestConstants.RECEIVER_NOTIFICATIONS_PATH)
    public List<NotificationSearchRow> searchReceivedNotification(
            @RequestParam(name = "recipientId") String recipientId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "senderId", required = false) String senderId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp
    ) {
        return svc.searchReceivedNotification(recipientId, startDate, endDate, senderId, status, subjectRegExp);
    }
}

package it.pagopa.pn.delivery.rest;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.api.dto.LegalFactsRow;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.*;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class PnReceivedNotificationsController implements
        PnDeliveryRestApi_methodGetReceivedNotification,
        PnDeliveryRestApi_methodGetReceivedNotificationDocuments,
        PnDeliveryRestApi_methodGetReceivedNotificationLegalFacts,
        PnDeliveryRestApi_methodSearchReceivedNotification
{

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_PATH)
    @JsonView(value = NotificationJsonViews.Sent.class )
    public Notification getReceivedNotification(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER ) String userId,
            @PathVariable( name = "iun") String iun
    ) {
        return null;
    }

    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_VIEWED_PATH )
    public ResponseEntity<Resource> getReceivedNotificationDocument(
            @PathVariable("iun") String iun,
            @PathVariable("documentIndex") int documentIndex,
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER, required = false ) String userId
    ) {
        return null;
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH)
    public List<LegalFactsRow> getReceivedNotificationLegalFacts(
            @RequestHeader(name = PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH ) String userId,
            @PathVariable( name = "iun") String iun
    ) {
        return null;
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH + "/{id}")
    public ResponseEntity<Resource> getReceivedNotificationLegalFact(
            @RequestHeader(name = PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH ) String userId,
            @PathVariable( name = "iun") String iun,
            @PathVariable( name = "id") String legalFactId
    ) {
        return null;
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_PATH)
    public List<NotificationSearchRow> searchReceivedNotification(
            @RequestParam(name = "recipientId") String recipientId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "senderId", required = false) String senderId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp
    ) {
        return null;
    }

}

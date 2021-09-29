package it.pagopa.pn.delivery.rest;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.*;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
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
    private final NotificationRetrieverService retrieveSvc;

    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc) {
        this.retrieveSvc = retrieveSvc;
    }


    @GetMapping(PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH)
    public List<NotificationSearchRow> searchReceivedNotification(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER ) String recipientId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "senderId", required = false) String senderId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp
    ) {
        return retrieveSvc.searchNotification( false, senderId, startDate, endDate, recipientId, status, subjectRegExp );
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_PATH)
    @JsonView(value = NotificationJsonViews.Sent.class )
    public Notification getReceivedNotification(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER ) String userId,
            @PathVariable( name = "iun") String iun
    ) {
        return retrieveSvc.getNotificationInformation( iun );
    }

    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_VIEWED_PATH )
    public ResponseEntity<Resource> getReceivedNotificationDocument(
            @PathVariable("iun") String iun,
            @PathVariable("documentIndex") int documentIndex,
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER, required = false ) String userId
    ) {
        ResponseEntity<Resource> resource = retrieveSvc.downloadDocument( iun, documentIndex, userId );
        return AttachmentRestUtils.prepareAttachment( resource, iun, "doc" + documentIndex );
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH)
    public List<LegalFactsListEntry> getReceivedNotificationLegalFacts(
            @RequestHeader(name = PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH ) String userId,
            @PathVariable( name = "iun") String iun
    ) {
        return retrieveSvc.listNotificationLegalFacts( iun );
    }

    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH + "/{id}")
    public ResponseEntity<Resource> getReceivedNotificationLegalFact(
            @RequestHeader(name = PnDeliveryRestConstants.NOTIFICATION_RECEIVED_LEGALFACTS_PATH ) String userId,
            @PathVariable( name = "iun") String iun,
            @PathVariable( name = "id") String legalFactId
    ) {
        ResponseEntity<Resource> resource = retrieveSvc.downloadLegalFact( iun, legalFactId );
        return AttachmentRestUtils.prepareAttachment( resource, iun, legalFactId.replaceFirst("\\.pdf$", "") );
    }

}

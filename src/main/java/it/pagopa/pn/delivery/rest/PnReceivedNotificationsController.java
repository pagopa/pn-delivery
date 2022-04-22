package it.pagopa.pn.delivery.rest;

import com.fasterxml.jackson.annotation.JsonView;
import it.pagopa.pn.api.dto.InputSearchNotificationDto;
import it.pagopa.pn.api.dto.NotificationSearchRow;
import it.pagopa.pn.api.dto.ResultPaginationDto;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.status.NotificationStatus;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetReceivedNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodGetReceivedNotificationDocuments;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodSearchReceivedNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.rest.utils.HandleNotFound;
import it.pagopa.pn.delivery.rest.utils.HandleValidation;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
public class PnReceivedNotificationsController implements
        PnDeliveryRestApi_methodGetReceivedNotification,
        PnDeliveryRestApi_methodGetReceivedNotificationDocuments,
        PnDeliveryRestApi_methodSearchReceivedNotification {
    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryConfigs cfg;
    public static final String VALIDATION_ERROR_STATUS = "Validation error";
    public static final String NOT_FOUND_ERROR_STATUS = "Not Found Error";

    public PnReceivedNotificationsController(NotificationRetrieverService retrieveSvc, PnDeliveryConfigs cfg) {
        this.retrieveSvc = retrieveSvc;
        this.cfg = cfg;
    }

    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATIONS_RECEIVED_PATH)
    public ResultPaginationDto<NotificationSearchRow,String> searchReceivedNotification(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER) String recipientId,
            //@RequestHeader(name = PnDeliveryRestConstants.UID_HEADER) String userId,
            @RequestParam(name = "startDate") Instant startDate,
            @RequestParam(name = "endDate") Instant endDate,
            @RequestParam(name = "senderId", required = false) String senderId,
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "subjectRegExp", required = false) String subjectRegExp,
            @RequestParam(name = "iunMatch", required = false) String iunMatch,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "nextPagesKey", required = false) String nextPagesKey
    ) {
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .senderReceiverId(recipientId)
                .startDate(startDate)
                .endDate(endDate)
                .filterId(senderId)
                .status(status)
                .subjectRegExp(subjectRegExp)
                .iunMatch(iunMatch)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();

        return retrieveSvc.searchNotification( searchDto ); //, userId );
    }

    @Override
    @GetMapping(PnDeliveryRestConstants.NOTIFICATION_RECEIVED_PATH)
    @JsonView(value = NotificationJsonViews.Sent.class)
    public Notification getReceivedNotification(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER) String userId,
            @PathVariable(name = "iun") String iun
    ) {
        return retrieveSvc.getNotificationAndNotifyViewedEvent(iun, userId);
    }

    @Override
    @GetMapping( PnDeliveryRestConstants.NOTIFICATION_VIEWED_PATH )
    public ResponseEntity<Resource> getReceivedNotificationDocument(
            @RequestHeader(name = PnDeliveryRestConstants.USER_ID_HEADER) String userId,
            @PathVariable("iun") String iun,
            @PathVariable("documentIndex") int documentIndex,
            ServerHttpResponse response
    ) {
        if(cfg.isDownloadWithPresignedUrl()){
            String redirectUrl = retrieveSvc.downloadDocumentWithRedirect(iun, documentIndex);
            //response.setStatusCode(HttpStatus.OK);
            //response.getHeaders().setLocation(URI.create( redirectUrl ));

            response.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            String responseString  = "{ \"url\": \"" + redirectUrl + "\"}";
            Resource resource = new ByteArrayResource( responseString.getBytes(StandardCharsets.UTF_8) );
            return ResponseEntity.ok( resource );
        }else {
            ResponseEntity<Resource> resource = retrieveSvc.downloadDocument(iun, documentIndex);
            return AttachmentRestUtils.prepareAttachment( resource, iun, "doc" + documentIndex );
        }
    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        return HandleValidation.handleValidationException(ex, VALIDATION_ERROR_STATUS);
    }

    @ExceptionHandler({PnNotFoundException.class})
    public ResponseEntity<ResErrorDto> handleNotFoundException(PnNotFoundException ex) {
        return HandleNotFound.handleNotFoundException( ex, NOT_FOUND_ERROR_STATUS );
    }
}

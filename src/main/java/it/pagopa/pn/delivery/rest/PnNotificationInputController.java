package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.events.ServiceLevelType;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.preload.PreloadRequest;
import it.pagopa.pn.api.dto.preload.PreloadResponse;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodReceiveNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.rest.model.ResErrorModel;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.delivery.svc.NotificationReceiverService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class PnNotificationInputController implements PnDeliveryRestApi_methodReceiveNotification {

    private final NotificationReceiverService svc;
    private final S3PresignedUrlService presignSvc;

    public PnNotificationInputController(NotificationReceiverService svc, S3PresignedUrlService presignSvc) {
        this.svc = svc;
        this.presignSvc = presignSvc;
    }

    @Override
    @PostMapping(PnDeliveryRestConstants.SEND_NOTIFICATIONS_PATH )
    public NewNotificationResponse receiveNotification(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @RequestBody @JsonView(value = NotificationJsonViews.New.class ) Notification notification
    ) {
        if( notification.getPhysicalCommunicationType() == null ) {
            notification = notification.toBuilder()
                    .physicalCommunicationType(ServiceLevelType.REGISTERED_LETTER_890)
                    .build();
        }

        Notification withSender = notification.toBuilder()
                .sender( NotificationSender.builder()
                        .paId( paId )
                        .build()
                )
                .build();

        return svc.receiveNotification( withSender );
    }

    @Override
    @PostMapping( PnDeliveryRestConstants.ATTACHMENT_PRELOAD_REQUEST)
    public PreloadResponse presignedUploadRequest(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @RequestBody PreloadRequest request
    ) {
        return presignSvc.presignedUpload( paId, request.getKey() );

    }

    @ExceptionHandler({PnValidationException.class})
    public ResponseEntity<Map> handleValidationException(PnValidationException ex){
        List<String> messages = ex.getValidationErrors().stream()
                .map(msg -> ResErrorModel.builder().message(msg.getMessage()).path(msg.getPropertyPath())
                        .toString()).collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(Collections.singletonMap("errors",messages));
    }
}

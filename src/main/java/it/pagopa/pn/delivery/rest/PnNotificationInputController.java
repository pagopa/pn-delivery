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
import it.pagopa.pn.delivery.rest.dto.ErrorDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import it.pagopa.pn.delivery.svc.S3PresignedUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.delivery.svc.NotificationReceiverService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class PnNotificationInputController implements PnDeliveryRestApi_methodReceiveNotification {

    public static final String NOTIFICATION_VALIDATION_ERROR_STATUS = "Notification validation error";
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
    public ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex){
        List<ErrorDto> listErrorDto = ex.getValidationErrors().stream()
                .map(msg ->
                                ErrorDto.builder()
                                        .message(msg.getMessage())
                                        .property(msg.getPropertyPath().toString())
                                        .code(msg.getConstraintDescriptor().getAnnotation().getClass().getSimpleName())
                                        .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ResErrorDto.builder()
                        .paNotificationId(ex.getValidationTargetId())
                        .status(NOTIFICATION_VALIDATION_ERROR_STATUS)
                        .errorDtoList(listErrorDto)
                        .build());
    }
}

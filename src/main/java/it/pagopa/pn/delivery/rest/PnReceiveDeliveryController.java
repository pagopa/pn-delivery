package it.pagopa.pn.delivery.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodReceiveNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.delivery.svc.receivenotification.NotificationReceiverService;

@RestController
public class PnReceiveDeliveryController implements PnDeliveryRestApi_methodReceiveNotification {


    private final NotificationReceiverService svc;

    public PnReceiveDeliveryController(NotificationReceiverService svc) {
        this.svc = svc;
    }

    @Override
    @PostMapping(PnDeliveryRestConstants.SENDER_NOTIFICATIONS_PATH )
    public NewNotificationResponse receiveNotification(
            @RequestHeader(name = PnDeliveryRestConstants.PA_ID_HEADER ) String paId,
            @RequestBody @JsonView(value = NotificationJsonViews.New.class ) Notification notification
    ) {

        Notification withSender = notification.toBuilder()
                .sender( NotificationSender.builder()
                        .paId( paId )
                        .build()
                )
                .build();

        return svc.receiveNotification( withSender );
    }

}

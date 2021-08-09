package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.NewNotificationResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationJsonViews;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.rest.PnDeliveryRestApi_methodReceiveNotification;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;

import it.pagopa.pn.delivery.DeliveryService;
import reactor.core.publisher.Mono;

@RestController
public class PnSentDeliveryController implements PnDeliveryRestApi_methodReceiveNotification {
	
    @Autowired
    private DeliveryService svc;

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

    /*@PostMapping("")
    @ResponseBody
    public Mono<ResponseEntity<NewNotificationResponse>> send(
            @RequestBody @JsonView(value = JsonViews.NotificationsView.Sent.class ) Notification notification,
            @RequestHeader("X-PagoPA-PN-PA") String paId
    ) {
        Notification withSender = notification.withSender( NotificationSender.builder().paId( paId ).build() );
        NewNotificationResponse addedNotification = svc.receiveNotification( notification );

        ResponseEntity<NewNotificationResponse> entity = ResponseEntity.ok().body( addedNotification );
        return Mono.just( entity );
    }*/



}

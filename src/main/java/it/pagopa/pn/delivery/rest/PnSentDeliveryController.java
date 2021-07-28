package it.pagopa.pn.delivery.rest;

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
import it.pagopa.pn.delivery.model.notification.Notification;
import it.pagopa.pn.delivery.model.notification.response.NotificationResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/delivery/notifications/sent/")
public class PnSentDeliveryController {

    @Autowired
    private DeliveryService svc;

    @PostMapping("/")
    @ResponseBody
    public Mono<NotificationResponse> send(
            @RequestBody @JsonView(value = Views.NotificationsView.Send.class ) Notification notification,
            @RequestHeader("X-PagoPA-PN-PA") String paId
    ) {
        Notification addedNotification = svc.receiveNotification( paId, notification );
        return Mono.just(new NotificationResponse(addedNotification.getIun(), addedNotification.getPaNotificationId()));
    }

    @GetMapping("/")
    @JsonView(value = Views.NotificationsView.Sent.class )
    public Mono<ResponseEntity<Notification>> getAllSentNotification(
            @RequestHeader("X-PagoPA-PN-PA") String paId
    ) {
        return Mono.just(ResponseEntity.ok( null ));
    }


}



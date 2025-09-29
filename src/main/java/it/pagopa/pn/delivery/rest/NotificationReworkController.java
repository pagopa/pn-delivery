package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.bo.v1.api.NotificationReworkApi;
import it.pagopa.pn.delivery.generated.openapi.server.bo.v1.dto.ReworkRequest;
import it.pagopa.pn.delivery.generated.openapi.server.bo.v1.dto.ReworkResponse;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ReworkInformation;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;


@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkController implements NotificationReworkApi {

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryPushClientImpl deliveryPushClient;
    private final PnSafeStorageClientImpl safeStorageClient;
    private final NotificationReworksDao notificationReworksDao;

    private final ModelMapper modelMapper;

    @Override
    public Mono<ResponseEntity<ReworkResponse>> notificationRework(String iun, Mono<ReworkRequest> reworkRequest, ServerWebExchange exchange) {
        //TODO aggiungere logs
        //TODO nullpointers?
        //TODO doOnErrors?
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, iun);
        log.info("[enter] notificationRework iun={} reworkRequest={}", iun, reworkRequest);
        return MDCUtils.addMDCToContextAndExecute(
                reworkRequest
                        .flatMap(request -> createReworkInfo(iun, request))
                        .flatMap(this::verifyIfReworkable)
                        .flatMap(this::checkFilesValidity)
                        .flatMap(this::saveToNotificationReworkTable)
                        .flatMap(this::sendAction)
                        .doOnError(PnNotificationNotFoundException.class, ex -> Mono.just(ResponseEntity.notFound()))
                        .then(Mono.empty())
        );
    }

    private Mono<ReworkInformation> sendAction(ReworkInformation info) {
        //TODO implementare
        return Mono.just(info);
    }

    private Mono<ReworkInformation> createReworkInfo(String iun, ReworkRequest request) {
        return Mono.just(ReworkInformation.builder()
                .reworkId("REWORK#" + UUID.randomUUID())
                .iun(iun)
                .request(request)
                .errors(new ArrayList<>())
                .build());
    }

    private Mono<ReworkInformation> saveToNotificationReworkTable(ReworkInformation info) {
        return notificationReworksDao.putItem(createNotificationReworkEntity(info)).then(Mono.just(info));
    }

    private NotificationReworksEntity createNotificationReworkEntity(ReworkInformation info) {
        return NotificationReworksEntity.builder()
                .iun(info.getIun())
                .reworkId(info.getReworkId())
                .status("CREATED")
                .createdAt(Instant.now())
                //TODO li avevo rimossi, riservono
                .invalidatedTimelineElementIds(null)
                .build();
    }

    private Mono<ReworkInformation> checkFilesValidity(ReworkInformation info) {
        return this.getNotification(info.getIun())
                .flatMap(notification -> Flux.fromIterable(notification.getDocuments())
                        //TODO come controllare l'errore dato da safe storage?
                        .flatMap(doc -> Mono.just(safeStorageClient.getFile(doc.getRef().getKey(), true)))
                        .then(Mono.just(info)));
    }

    private Mono<InternalNotification> getNotification(String iun) {
        return Mono.just(retrieveSvc.getNotificationInformation(iun, false, false));
    }

    private Mono<ReworkInformation> verifyIfReworkable(ReworkInformation info) {
        return deliveryPushClient.notificationRework(info.getIun(), mapReworkRequestForDeliveryPush(info.getRequest()))
                .flatMap(response -> {
                    log.info("Rework request accepted for iun={}", info.getIun());
                    if (Objects.nonNull(response.getError())) {
                        info.getErrors().add(response.getError());
                    }
                    info.setResponse(response);
                    return Mono.just(info);
                });
    }

    private it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkRequest mapReworkRequestForDeliveryPush(ReworkRequest request) {
        return modelMapper.map(request, it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkRequest.class);
    }

}

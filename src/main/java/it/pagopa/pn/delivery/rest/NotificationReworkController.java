package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkResponse;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.api.NotificationReworkApi;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
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
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkController implements NotificationReworkApi {

    private final List<String> ACCEPTED_STATUSES_MONO = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> ACCEPTED_STATUSES_MULTI = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CREATED = "STATUS_CREATED";
    public static final String CAUSE_INVALID_TIMELINE_ELEMENT = "INVALID_TIMELINE_ELEMENT";
    public static final String CAUSE_REWORK_ALREADY_PRESENT = "REWORK_REQUEST_ALREADY_PRESENT";

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryPushClientImpl deliveryPushClient;
    private final PnSafeStorageClientImpl safeStorageClient;
    private final NotificationReworksDao notificationReworksDao;
    private final PnDeliveryConfigs config;

    private final ModelMapper modelMapper;

//    @Override
//    public Mono<ResponseEntity<ReworkItemsResponse>> retrieveNotificationRework(String iun, final ServerWebExchange exchange) {
//        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, iun);
//        log.info("[START] notificationRework iun={} reworkRequest={}", iun, reworkRequest);
//        return MDCUtils.addMDCToContextAndExecute(
//                reworkRequest.
//                        thenReturn(ResponseEntity.ok(new ReworkResponse()))
//                        .doOnError(ex -> log.error("Errore durante notificationRework per iun={}: {}", iun, ex.getMessage()))
//                        .doOnSuccess(resp -> log.info("[END] notificationRework iun={} response={}", iun, resp))
//        );
//    }

}

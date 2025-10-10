package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.api.NotificationReworkApi;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkItem;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkController implements NotificationReworkApi {

    private final NotificationReworksDao notificationReworksDao;
    private final PnDeliveryConfigs configs;

    @Override
    public Mono<ResponseEntity<ReworkItemsResponse>> retrieveNotificationRework(String iun, String reworkId,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, iun);
        log.info("[START] notificationRework iun={} reworkId={}", iun, reworkId);
        return MDCUtils.addMDCToContextAndExecute(
                this.findRework(iun, reworkId)
                        .flatMap(list -> this.buildResponse(iun, list))
                        .doOnError(ex -> log.error("Errore durante notificationRework per iun={}: {}", iun, ex.getMessage()))
                        .doOnSuccess(resp -> log.info("[END] notificationRework iun={} response={}", iun, resp))
        );
    }

    private Mono<ResponseEntity<ReworkItemsResponse>> buildResponse(String iun, List<NotificationReworksEntity> notificationReworksEntities) {
        ReworkItemsResponse reworkResponse = new ReworkItemsResponse();
        reworkResponse.iun(iun);
        List<ReworkItem> list = Optional.ofNullable(notificationReworksEntities).orElse(new ArrayList<>()).stream()
                .map(elem -> {
                    ReworkItem item = new ReworkItem();
                    item.setReworkId(elem.getReworkId());
                    item.invalidatedTimelineElementIds(elem.getInvalidatedTimelineElementIds());
                    item.setStatus(ReworkItem.StatusEnum.valueOf(elem.getStatus()));
                    item.errors(new ArrayList<>());
                    item.expectedStatusCode(elem.getExpectedStatusCodes().get(0));
                    item.expectedDeliveryFailureCause(elem.getExpectedDeliveryFailureCause());
                    item.setReason(elem.getReason());
                    item.setCreatedAt(String.valueOf(elem.getCreatedAt()));
                    item.setUpdatedAt(String.valueOf(elem.getUpdatedAt()));
                    return item;
                })
                .collect(Collectors.toList());
        reworkResponse.items(list);

        return Mono.just(ResponseEntity.ok(reworkResponse));
    }

    private Mono<List<NotificationReworksEntity>> findRework(String iun, String reworkId) {
        return StringUtils.hasText(reworkId) ?
                this.getAllReworksByIunAndReworkId(iun, reworkId) :
                this.getAllReworksByIun(iun);
    }

    private Mono<List<NotificationReworksEntity>> getAllReworksByIunAndReworkId(String iun, String reworkId) {
        return notificationReworksDao.findByIunAndReworkId(iun, reworkId)
                .map(List::of)
                .defaultIfEmpty(List.of());
    }

    private Mono<List<NotificationReworksEntity>> getAllReworksByIun(String iun) {
        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();
        List<NotificationReworksEntity> allEntities = new ArrayList<>();

        return Mono.defer(() -> fetchAllPages(iun, lastEvaluateKey, configs.getReworkRetrievePageLimit(), allEntities));
    }

    private Mono<List<NotificationReworksEntity>> fetchAllPages(
            String iun,
            Map<String, AttributeValue> lastEvaluateKey,
            int pageSize,
            List<NotificationReworksEntity> accumulator
    ) {
        return notificationReworksDao.findByIun(iun, lastEvaluateKey, pageSize)
                .flatMap(page -> {
                    accumulator.addAll(page.items());
                    if (page.lastEvaluatedKey() == null || page.lastEvaluatedKey().isEmpty()) {
                        return Mono.just(accumulator);
                    } else {
                        return fetchAllPages(iun, page.lastEvaluatedKey(), pageSize, accumulator);
                    }
                });
    }

}
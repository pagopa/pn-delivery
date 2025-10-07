package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnConflictException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkError;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.api.NotificationReworkApi;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkRequest;
import it.pagopa.pn.delivery.generated.openapi.server.rework.v1.dto.ReworkResponse;
import it.pagopa.pn.delivery.middleware.notificationdao.NotificationReworksDao;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksEntity;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationReworksErrorEntity;
import it.pagopa.pn.delivery.models.ReworkInformation;
import it.pagopa.pn.delivery.pnclient.deliverypush.PnDeliveryPushClientImpl;
import it.pagopa.pn.delivery.pnclient.safestorage.PnSafeStorageClientImpl;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkController implements NotificationReworkApi {

    private final List<String> ACCEPTED_STATUSES_MONO = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    private final List<String> ACCEPTED_STATUSES_MULTI = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");

    private final NotificationRetrieverService retrieveSvc;
    private final PnDeliveryPushClientImpl deliveryPushClient;
    private final PnSafeStorageClientImpl safeStorageClient;
    private final NotificationReworksDao notificationReworksDao;
    private final PnDeliveryConfigs config;

    private final ModelMapper modelMapper;

    @Override
    public Mono<ResponseEntity<ReworkResponse>> notificationRework(String iun, Mono<ReworkRequest> reworkRequest, ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, iun);
        log.info("[START] notificationRework iun={} reworkRequest={}", iun, reworkRequest);
        return MDCUtils.addMDCToContextAndExecute(
                reworkRequest
                        .flatMap(request -> {
                            log.info("Ricevuta richiesta di rework per iun={} con request={}", iun, request);
                            return createReworkInfo(iun, request);
                        })
                        .flatMap(this::getNotification)
                        .switchIfEmpty(Mono.error(new PnNotificationNotFoundException("Notification with iun=" + iun + " not found")))
                        .flatMap(this::verifyReworkRequestAlreadyPresent)
                        .flatMap(this::saveToNotificationReworkTable)
                        .flatMap(this::verifyAcceptedStatuses)
                        .flatMap(this::checkFilesValidity)
                        .flatMap(this::verifyIfReworkable)
                        .flatMap(this::createResponse)
                        .doOnError(ex -> log.error("Errore durante notificationRework per iun={}: {}", iun, ex.getMessage()))
                        .doOnSuccess(resp -> log.info("[END] notificationRework iun={} response={}", iun, resp))
        );
    }

    private Mono<ResponseEntity<ReworkResponse>> createResponse(ReworkInformation info) {
        log.info("Creazione risposta per rework iun={} reworkId={}", info.getIun(), info.getReworkId());
        return info.getErrors().isEmpty() ? Mono.just(ResponseEntity.ok().build()) : updateNotificationReworkWithErrors(info).thenReturn(ResponseEntity.badRequest().build());
    }

    private Mono<ReworkInformation> verifyAcceptedStatuses(ReworkInformation info) {
        log.info("Verifica status accettati per iun={} status={}", info.getIun(), info.getNotification().getNotificationStatus());
        return Mono.just(info.getNotification())
                .flatMap(notification -> {
                    if (notification.getRecipients().size() == 1) {
                        if (!ACCEPTED_STATUSES_MONO.contains(notification.getNotificationStatus().name())) {
                            log.warn("Status non accettato per mono-recipient: {}", notification.getNotificationStatus());
                            updateNotificationReworkStatusToError(info, "ERROR", "NOTIFICATION_STATUS_NOT_ACCEPTED", "Notification status " + notification.getNotificationStatus() + " not accepted for mono recipient, " + ACCEPTED_STATUSES_MONO);
                            return Mono.error(new PnNotificationNotFoundException("Notification status not accepted for mono recipient"));
                        }
                    } else {
                        if (!ACCEPTED_STATUSES_MULTI.contains(notification.getNotificationStatus().name())) {
                            log.warn("Status non accettato per multi-recipient: {}", notification.getNotificationStatus());
                            updateNotificationReworkStatusToError(info, "ERROR", "NOTIFICATION_STATUS_NOT_ACCEPTED", "Notification status " + notification.getNotificationStatus() + " not accepted for mono recipient, " + ACCEPTED_STATUSES_MULTI);
                            return Mono.error(new PnNotificationNotFoundException("Notification status not accepted for multi recipient"));
                        }
                    }
                    return Mono.just(info);
                });
    }

    private Mono<ReworkInformation> verifyReworkRequestAlreadyPresent(ReworkInformation info) {
        log.info("Verifica presenza rework precedente per iun={}", info.getIun());
        return notificationReworksDao.findLatestByIun(info.getIun())
                .flatMap(reworkEntity -> {
                    if (!reworkEntity.getStatus().equals("DONE") && !reworkEntity.getStatus().equals("ERROR")) {
                        log.info("Rework request già presente per iun={} reworkId={}", info.getIun(), reworkEntity.getReworkId());
                        return Mono.error(new PnConflictException("REWORK_REQUEST_ALREADY_PRESENT", "Rework request con iun=" + info.getIun() + " e reworkId=" + info.getReworkId() + " già presente"));
                    }
                    info.setPreviousReworkEntity(reworkEntity);
                    return Mono.just(info);
                });
    }

    private Mono<ReworkInformation> createReworkInfo(String iun, ReworkRequest request) {
        log.info("Creazione ReworkInformation per iun={}", iun);
        return Mono.just(ReworkInformation.builder()
                .reworkId("REWORK#" + UUID.randomUUID())
                .iun(iun)
                .request(request)
                .errors(new ArrayList<>())
                .build());
    }

    private Mono<ReworkInformation> saveToNotificationReworkTable(ReworkInformation info) {
        log.info("Salvataggio rework su tabella per iun={} reworkId={}", info.getIun(), info.getReworkId());
        return notificationReworksDao.putIfAbsent(createNotificationReworkEntity(info)).then(Mono.just(info));
    }

    private NotificationReworksEntity createNotificationReworkEntity(ReworkInformation info) {
        String reworkIdIndex = "0";
        if (Objects.nonNull(info.getPreviousReworkEntity())) {
            if (info.getPreviousReworkEntity().getStatus().equals("ERROR")) {
                reworkIdIndex = info.getPreviousReworkEntity().getIdx();
            } else {
                reworkIdIndex = String.valueOf(Integer.parseInt(info.getPreviousReworkEntity().getIdx()) + 1);
            }
        }
        info.setReworkId("REWORK_"  + reworkIdIndex + "_" + UUID.randomUUID());
        log.info("Creazione NotificationReworksEntity per iun={} reworkId={}", info.getIun(), info.getReworkId());
        return NotificationReworksEntity.builder()
                .iun(info.getIun())
                .reworkId(info.getReworkId())
                .status("CREATED")
                .createdAt(Instant.now())
                .expectedStatusCodes(List.of(info.getRequest().getExpectedStatusCode()))
                .expectedDeliveryFailureCause(info.getRequest().getExpectedDeliveryFailureCause())
                .reason(info.getRequest().getReason())
                .build();
    }

    private Mono<ReworkInformation> checkFilesValidity(ReworkInformation info) {
        log.info("Verifica validità file per iun={}", info.getIun());
        return Flux.fromIterable(info.getNotification().getDocuments())
                .flatMap(doc -> {
                    info.setCurrentDoc(doc);
                    log.info("Verifica file: {}", doc.getRef().getKey());
                    return Mono.just(safeStorageClient.getFile(doc.getRef().getKey(), true));
                })
                .flatMap(docResp -> {
                    if (docResp.getRetentionUntil().minusDays(config.getDocumentExpiringDateRange()).isBefore(OffsetDateTime.now())) {
                        log.debug("Documento {} con retention in data {}", info.getCurrentDoc().getRef().getKey(), docResp.getRetentionUntil());
                        info.getErrors().add(new ReworkError().errorType("EXPIRED_ATTACHMENT").message("l'allegato " + info.getCurrentDoc().getRef().getKey() + " non è più disponibile"));
                    }
                    return Mono.empty();
                })
                .doOnError(PnHttpResponseException.class, (error) -> {
                    if (error.getStatusCode() == HttpStatus.GONE.value()) {
                        log.warn("Allegato scaduto: {}", info.getCurrentDoc().getRef().getKey());
                        info.getErrors().add(new ReworkError().errorType("EXPIRED_ATTACHMENT").message("l'allegato " + info.getCurrentDoc().getRef().getKey() + " non è più disponibile"));
                    }
                })
                .doOnError(ex -> log.error("Errore durante verifica file per iun={}: {}", info.getIun(), ex.getMessage()))
                .then(Mono.just(info));
    }

    private Mono<ReworkInformation> getNotification(ReworkInformation info) {
        log.info("Recupero notification per iun={}", info.getIun());
        return Mono.just(retrieveSvc.getNotificationInformation(info.getIun(), false, false))
                .flatMap(notification -> {
                    info.setNotification(notification);
                    return Mono.just(info);
                });
    }

    private Mono<ReworkInformation> verifyIfReworkable(ReworkInformation info) {
        log.info("Verifica se rework è accettabile per iun={}", info.getIun());
        return deliveryPushClient.notificationRework(info.getIun(), mapReworkRequestForDeliveryPush(info.getRequest()))
                .flatMap(response -> {
                    log.info("Rework request accettata per iun={}", info.getIun());
                    ReworkError error = response.getError();
                    if (error != null) {
                        if ("INVALID_TIMELINE_ELEMENT".equals(error.getErrorType())) {
                            log.warn("Errore INVALID_TIMELINE_ELEMENT nella risposta rework: {}", error);
                            return Mono.error(new PnConflictException("INVALID_TIMELINE_ELEMENT", error.getMessage()));
                        } else {
                            info.getErrors().add(error);
                        }
                    }
                    return Mono.just(info);
                });
    }

    private it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkRequest mapReworkRequestForDeliveryPush(ReworkRequest request) {
        log.debug("Mapping ReworkRequest per deliveryPush");
        return modelMapper.map(request, it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.ReworkRequest.class);
    }

    private void updateNotificationReworkStatusToError(ReworkInformation info, String status, String cause, String description) {
        log.error("Aggiornamento stato rework a ERROR per iun={} reworkId={} causa={}", info.getIun(), info.getReworkId(), cause);
        NotificationReworksEntity entity = NotificationReworksEntity.builder()
                .iun(info.getIun())
                .reworkId(info.getReworkId())
                .status(status)
                .errors(List.of(NotificationReworksErrorEntity.builder()
                        .cause(cause)
                        .description(description)
                        .build()))
                .build();

        notificationReworksDao.update(entity);
    }

    private Mono<NotificationReworksEntity> updateNotificationReworkWithErrors(ReworkInformation info) {
        log.error("Aggiornamento rework con errori per iun={} reworkId={}", info.getIun(), info.getReworkId());
        NotificationReworksEntity entity = NotificationReworksEntity.builder()
                .iun(info.getIun())
                .reworkId(info.getReworkId())
                .status("ERROR")
                .errors(info.getErrors().stream().map(err -> NotificationReworksErrorEntity.builder()
                        .cause(err.getErrorType())
                        .description(err.getMessage())
                        .build()).toList())
                .build();

        return notificationReworksDao.update(entity);
    }

}

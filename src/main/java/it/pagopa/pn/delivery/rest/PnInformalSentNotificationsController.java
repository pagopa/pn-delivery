package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderInformalReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatus;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.NotificationRetrieverService;
import it.pagopa.pn.delivery.svc.search.CampaignAuthValidator;
import it.pagopa.pn.delivery.svc.search.NotificationSearchService;
import it.pagopa.pn.delivery.utils.InformalNotificationStatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@Slf4j
public class PnInformalSentNotificationsController implements SenderInformalReadWebApi {

    private final NotificationSearchService retrieveSvc;
    private final ModelMapper modelMapper;
    private final CampaignAuthValidator campaignAuthValidator;

    public PnInformalSentNotificationsController(NotificationSearchService retrieveSvc,
                                                 ModelMapper modelMapper,
                                                 CampaignAuthValidator campaignAuthValidator) {
        this.retrieveSvc = retrieveSvc;
        this.modelMapper = modelMapper;
        this.campaignAuthValidator = campaignAuthValidator;
    }

    @Override
    public ResponseEntity<InformalNotificationSearchResponse> searchInformalSentNotification(String xPagopaPnUid,
                                                                                             CxTypeAuthFleet xPagopaPnCxType,
                                                                                             String xPagopaPnCxId,
                                                                                             String campaignId,
                                                                                             OffsetDateTime startDate,
                                                                                             OffsetDateTime endDate,
                                                                                             List<String> xPagopaPnCxGroups,
                                                                                             String recipientId,
                                                                                             String iunMatch,
                                                                                             InformalNotificationStatus status,
                                                                                             String group,
                                                                                             Boolean viewed,
                                                                                             Boolean delivered,
                                                                                             Integer size,
                                                                                             String nextPagesKey) {
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchInformalSentNotification campaignId={}", campaignId)
                .iun(iunMatch)
                .build();
        logEvent.log();

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .byCampaign(true)
                .campaignId(campaignId)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .iunMatch(iunMatch)
                .informalStatuses(status == null ? List.of() : List.of(status))
                // se è specificato un singolo gruppo come filtro lo si usa, altrimenti si filtra sui gruppi dell'utente
                .groups(StringUtils.hasText(group) ? List.of(group) : xPagopaPnCxGroups)
                .viewed(viewed)
                .delivered(delivered)
                // la ricerca per campagna è per definizione bonaria: si forza esplicitamente il filtro INFORMAL
                .communicationType(NotificationSearchCommunicationType.INFORMAL)
                .receiverIdIsOpaque(false)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> serviceResult;
        InformalNotificationSearchResponse response = new InformalNotificationSearchResponse();
        try {
            // Autorizzazione campagna -> mittente (WI-US4.10): la campagna deve appartenere a xPagopaPnCxId.
            // Solleva PnForbiddenException quando la verifica fallisce (implementazione stub, vedi validator).
            campaignAuthValidator.assertCampaignBelongsToSender(campaignId, xPagopaPnCxId);
            serviceResult = retrieveSvc.searchNotification(searchDto, null, null);
            // la validazione di dominio deve stare fuori dal map(): ModelMapper incapsula
            // le eccezioni del converter in MappingException, perdendo il codice errore dedicato
            InformalNotificationStatusValidator.assertInformalCompatible(serviceResult);
            response = modelMapper.map(serviceResult, InformalNotificationSearchResponse.class);
            logEvent.generateSuccess().log();
        } catch (PnRuntimeException exc) {
            logEvent.generateFailure("" + exc.getProblem()).log();
            throw exc;
        }
        return ResponseEntity.ok(response);
    }
}

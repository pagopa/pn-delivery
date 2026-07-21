package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.SenderInformalReadWebApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationSearchResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.NotificationSearchCommunicationType;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
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
                                                                                             InformalNotificationStatusV1 status,
                                                                                             Boolean viewed,
                                                                                             Boolean delivered,
                                                                                             Integer size,
                                                                                             String nextPagesKey) {

        InputSearchNotificationDto searchDto = new InputSearchNotificationDto().toBuilder()
                .byCampaign(true)
                .campaignId(campaignId)
                .senderReceiverId(xPagopaPnCxId)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .iunMatch(iunMatch)
                .informalStatuses(status == null ? List.of() : List.of(status))
                .groups(xPagopaPnCxGroups)
                .viewed(viewed)
                .delivered(delivered)
                .communicationType(NotificationSearchCommunicationType.INFORMAL)
                .receiverIdIsOpaque(false)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();

        ResultPaginationDto<NotificationSearchRow, String> serviceResult;
        InformalNotificationSearchResponse response;
        try {
            campaignAuthValidator.checkCampaignIsFromSender(campaignId, xPagopaPnCxId);
            serviceResult = retrieveSvc.searchNotification(searchDto, null, null);
            InformalNotificationStatusValidator.assertInformalCompatible(serviceResult);
            response = modelMapper.map(serviceResult, InformalNotificationSearchResponse.class);
        } catch (PnRuntimeException exc) {
            log.error("Error searching informal sent notifications for campaignId={} senderId={} exc={}", campaignId, xPagopaPnCxId, exc.getMessage(), exc);
            throw exc;
        }
        return ResponseEntity.ok(response);
    }
}

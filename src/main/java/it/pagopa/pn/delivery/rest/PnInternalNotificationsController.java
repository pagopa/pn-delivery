package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InputSearchNotificationDto;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.svc.StatusService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
public class PnInternalNotificationsController implements InternalOnlyApi {

    private final NotificationRetrieverService retrieveSvc;
    private final StatusService statusService;

    private final ModelMapperFactory modelMapperFactory;

    public PnInternalNotificationsController(NotificationRetrieverService retrieveSvc, StatusService statusService, ModelMapperFactory modelMapperFactory) {
        this.retrieveSvc = retrieveSvc;
        this.statusService = statusService;
        this.modelMapperFactory = modelMapperFactory;
    }

    @Override
    public ResponseEntity<SentNotification> getSentNotificationPrivate(String iun) {
        InternalNotification notification = retrieveSvc.getNotificationInformation(iun, false);
        ModelMapper mapper = modelMapperFactory.createModelMapper(InternalNotification.class, SentNotification.class);
        SentNotification sentNotification = mapper.map(notification, SentNotification.class);

        int recIdx = 0;
        for (NotificationRecipient rec : sentNotification.getRecipients()) {
            rec.setInternalId(notification.getRecipientIds().get(recIdx));
            recIdx += 1;
        }

        return ResponseEntity.ok(sentNotification);
    }

    @Override
    public ResponseEntity<Void> updateStatus(RequestUpdateStatusDto requestUpdateStatusDto) {
        String logMessage = String.format(
                "Update status for iun=%s nextStatus=%s", requestUpdateStatusDto.getIun(), requestUpdateStatusDto.getNextStatus()
        );
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_STATUS, logMessage)
                .iun(requestUpdateStatusDto.getIun())
                .build();
        logEvent.log();
        try {
            statusService.updateStatus(requestUpdateStatusDto);
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(logMessage).log();
            throw exc;
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public  ResponseEntity<NotificationSearchResponse> searchNotificationsPrivate(Date startDate, Date endDate,
                                  String recipientId, Boolean recipientIdOpaque, List<NotificationStatus> status,
                                  Integer size, String nextPagesKey) {

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent = auditLogBuilder
                .before(PnAuditLogEventType.AUD_NT_SEARCH_SND, "searchNotificationsPrivate")
                .build();
        InputSearchNotificationDto searchDto = new InputSearchNotificationDto.Builder()
                .bySender(false)
                .startDate(startDate.toInstant())
                .endDate(endDate.toInstant())
                .filterId(recipientId)
                .statuses(status==null?List.of():status)
                .receiverIdIsOpaque(recipientIdOpaque)
                .size(size)
                .nextPagesKey(nextPagesKey)
                .build();
        ResultPaginationDto<NotificationSearchRow,String> serviceResult;
        NotificationSearchResponse response = new NotificationSearchResponse();
        try {
            serviceResult =  retrieveSvc.searchNotification( searchDto );
            ModelMapper mapper = modelMapperFactory.createModelMapper(ResultPaginationDto.class, NotificationSearchResponse.class );
            response = mapper.map( serviceResult, NotificationSearchResponse.class );
            logEvent.generateSuccess().log();
        } catch (Exception exc) {
            logEvent.generateFailure(exc.getMessage()).log();
            throw exc;
        }
        return ResponseEntity.ok( response );

    }
}

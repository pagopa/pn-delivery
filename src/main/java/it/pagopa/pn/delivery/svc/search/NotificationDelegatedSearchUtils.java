package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.DelegateType;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.MandateByDelegatorRequestDto;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NotificationDelegatedSearchUtils {

    private final PnMandateClientImpl mandateClient;

    public NotificationDelegatedSearchUtils(PnMandateClientImpl mandateClient) {
        this.mandateClient = mandateClient;
    }

    public List<NotificationDelegationMetadataEntity> checkMandates(List<NotificationDelegationMetadataEntity> queryResult,
                                                                    InputSearchNotificationDelegatedDto searchDto) {
        if (CollectionUtils.isEmpty(queryResult)) {
            log.debug("skip check mandates - query result is empty");
            return queryResult;
        }
        List<InternalMandateDto> mandates = getMandates(queryResult, searchDto);
        if (mandates.isEmpty()) {
            log.info("no valid mandate found");
            return Collections.emptyList();
        }
        Map<String, InternalMandateDto> mapMandates = mandates.stream()
                .collect(Collectors.toMap(InternalMandateDto::getMandateId, Function.identity()));
        return queryResult.stream()
                .filter(row -> isMandateValid(mapMandates.get(row.getMandateId()), row))
                .toList();
    }

    private boolean isMandateValid(InternalMandateDto mandate, NotificationDelegationMetadataEntity entity) {
        if (mandate == null) {
            return false;
        }
        Instant mandateStartDate = mandate.getDatefrom() != null ? Instant.parse(mandate.getDatefrom()) : null;
        Instant mandateEndDate = mandate.getDateto() != null ? Instant.parse(mandate.getDateto()) : null;
        return entity.getRecipientId().equals(mandate.getDelegator())
                && (mandateStartDate == null || entity.getSentAt().compareTo(mandateStartDate) >= 0) // sent after start mandate
                && (mandateEndDate == null || entity.getSentAt().compareTo(mandateEndDate) <= 0) // sent before end mandate
                && (CollectionUtils.isEmpty(mandate.getVisibilityIds()) || mandate.getVisibilityIds().contains(entity.getSenderId()));
    }

    private List<InternalMandateDto> getMandates(List<NotificationDelegationMetadataEntity> queryResult,
                                                 InputSearchNotificationDelegatedDto searchDto){
        List<MandateByDelegatorRequestDto> requestBody = queryResult.stream()
                .map(row -> {
                    MandateByDelegatorRequestDto requestDto = new MandateByDelegatorRequestDto();
                    requestDto.setMandateId(row.getMandateId());
                    requestDto.setDelegatorId(row.getRecipientId());
                    return requestDto;
                })
                .distinct()
                .toList();
        return mandateClient.listMandatesByDelegators(DelegateType.PG, searchDto.getCxGroups(), requestBody);
    }
}

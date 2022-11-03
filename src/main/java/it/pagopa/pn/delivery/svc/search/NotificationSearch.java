package it.pagopa.pn.delivery.svc.search;

import it.pagopa.pn.delivery.generated.openapi.clients.datavault.model.BaseRecipientDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationSearchRow;
import it.pagopa.pn.delivery.middleware.notificationdao.EntityToDtoNotificationMetadataMapper;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationMetadataEntity;
import it.pagopa.pn.delivery.models.ResultPaginationDto;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class NotificationSearch {

    private final PnDataVaultClientImpl dataVaultClient;
    protected final EntityToDtoNotificationMetadataMapper entityToDto;

    protected NotificationSearch(PnDataVaultClientImpl dataVaultClient, EntityToDtoNotificationMetadataMapper entityToDto) {
        this.dataVaultClient = dataVaultClient;
        this.entityToDto = entityToDto;
    }


    public abstract ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> searchNotificationMetadata();


    protected void deanonimizeResults(ResultPaginationDto<NotificationSearchRow, PnLastEvaluatedKey> globalResult){
        // faccio richiesta a data-vault per restituire i CF non opachi al FE
        if ( !Collections.emptyList().equals( globalResult.getResultsPage() ) ) {
            Set<String> opaqueTaxIds = globalResult.getResultsPage().stream()
                    .map( NotificationSearchRow::getRecipients )
                    .flatMap( Collection::stream )
                    .collect( Collectors.toSet() );
            if (!opaqueTaxIds.isEmpty()) {
                log.debug( "Opaque tax ids={}", opaqueTaxIds );
                List<BaseRecipientDto> dataVaultResults = dataVaultClient.getRecipientDenominationByInternalId(new ArrayList<>(opaqueTaxIds));
                if ( !dataVaultResults.isEmpty() ) {
                    for (NotificationSearchRow searchRow : globalResult.getResultsPage()) {
                        List<String> realTaxIds = new ArrayList<>();
                        for (String internalId : searchRow.getRecipients() ) {
                            Optional<BaseRecipientDto> match = dataVaultResults.stream().filter(r -> internalId.equals( r.getInternalId() ) ).findFirst();
                            match.ifPresent(baseRecipientDto -> realTaxIds.add(baseRecipientDto.getTaxId()));
                        }
                        searchRow.setRecipients( realTaxIds );
                    }
                } else {
                    log.error( "No result from data-vault for internalIds={}", opaqueTaxIds );
                }
            }
        }
    }
}

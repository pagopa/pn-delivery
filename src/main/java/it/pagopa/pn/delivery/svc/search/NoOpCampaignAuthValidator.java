package it.pagopa.pn.delivery.svc.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementazione <b>stub</b> di {@link CampaignAuthValidator} (WI-US4.10).
 * <p>
 * In assenza di una sorgente dati delle campagne in pn-delivery, questa implementazione non esegue
 * alcun controllo effettivo: registra soltanto la richiesta di verifica. Va sostituita non appena
 * sarà disponibile il meccanismo per risolvere la relazione {@code campaignId → senderId}.
 */
@Slf4j
@Component
public class NoOpCampaignAuthValidator implements CampaignAuthValidator {

    @Override
    public void assertCampaignBelongsToSender(String campaignId, String senderId) {
        // TODO WI-US4.10: implementare la verifica reale "la campagna appartiene al mittente".
        //  In pn-delivery non esiste ancora una sorgente dati campagne; l'implementazione dipende da
        log.warn("Campaign ownership check is not implemented yet (stub). campaignId={} senderId={}",
                campaignId, senderId);
    }
}

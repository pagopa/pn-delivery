package it.pagopa.pn.delivery.svc.search;

/**
 * Punto di estensione per l'autorizzazione "la campagna appartiene al mittente" (WI-US4.10).
 * <p>
 * La ricerca delle notifiche bonarie per campagna è consentita solo all'ente mittente proprietario
 * della campagna: prima di eseguire la ricerca occorre verificare che la {@code campaignId} richiesta
 * appartenga al {@code senderId} ({@code xPagopaPnCxId}) chiamante.
 * <p>
 * <b>Stato attuale.</b> In pn-delivery non esiste ancora una sorgente dati delle campagne
 * (registro/tabella/client). Questa interfaccia isola il contratto di autorizzazione così che il
 * controller possa già invocarlo; l'implementazione effettiva è demandata a sviluppi non ancora
 * disponibili (vedi {@link NoOpCampaignAuthValidator}).
 */
public interface CampaignAuthValidator {

    /**
     * Verifica che la campagna appartenga al mittente; in caso contrario deve sollevare
     * {@code PnForbiddenException}.
     *
     * @param campaignId identificativo della campagna richiesta
     * @param senderId   identificativo del mittente chiamante ({@code xPagopaPnCxId})
     */
    void assertCampaignBelongsToSender(String campaignId, String senderId);
}

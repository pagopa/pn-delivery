package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@AllArgsConstructor
public class FeatureFlagUtils {
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public boolean isPhysicalAddressLookupEnabled() {
        if(pnDeliveryConfigs.getPhysicalAddressLookupStartDate() == null) {
            log.error("The parameter PhysicalAddressLookupStartDate is not configured, feature is always deactivated.");
            return false;
        }

        Instant now = Instant.now();
        return pnDeliveryConfigs.getPhysicalAddressLookupStartDate().compareTo(now) <= 0;
    }

    /**
     * Metodo che verifica se per l'attualizzazione dei costi della notifica è possibile contattare il nuovo servizio.
     *
     * @param sentAt Data di invio della notifica.
     * @return true se la data di invio è successiva alla data di integrazione del nuovo servizio, false altrimenti.
     * **/
    public boolean isIntegrationWithNewCostServiceEnabled(Instant sentAt) {
        if(pnDeliveryConfigs.getNewCostServiceActivationDate() == null) {
            log.warn("The parameter newCostServiceActivationDate is not configured, feature is always deactivated.");
            return false;
        }

        return pnDeliveryConfigs.getNewCostServiceActivationDate().compareTo(sentAt) <= 0;
    }

    /**
      * Metodo che verifica se è abilitato il monitoraggio del nuovo servizio di costi.
      * Il monitoraggio è abilitato solo se è l'integrazione con il nuovo servizio di costi è ancora disattivata e se la data di invio della notifica è successiva alla data di abilitazione del monitoraggio.
      *
      * @param sentAt Data di invio della notifica.
      * @return true se il monitoraggio è abilitato, false altrimenti.
     */
    public boolean isMonitoringOfNewCostServiceEnabled(Instant sentAt) {
        // Se già mi sono integrato con il nuovo servizio di costi, non ha senso monitorare l'effettivo utilizzo del nuovo servizio, quindi il monitoraggio sarà disabilitato.
        if(isIntegrationWithNewCostServiceEnabled(sentAt)) {
            log.debug("Integration with new cost service is enabled, so monitoring will be skipped.");
            return false;
        }

        if (pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate() == null) {
            log.warn("The parameter newCostServiceNotificationProcessingStartDate is not configured, monitoring feature is always deactivated.");
            return false;
        }

        return pnDeliveryConfigs.isNewCostServiceMonitoringEnabled() && pnDeliveryConfigs.getNewCostServiceNotificationProcessingStartDate().compareTo(sentAt) <= 0;
    }
}

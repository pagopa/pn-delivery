package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;

/**
 * Verifica fail-fast dell'invariante di dominio sul boundary dei flussi legali (mittente/delegato).
 * <p>
 * La riga di ricerca interna ({@link NotificationSearchRow}) trasporta lo stato unificato
 * {@link UnifiedNotificationStatus}, valido sia per le notifiche legali sia per le bonarie. I flussi
 * legali, però, per costruzione del dominio (filtro {@code communicationType} applicato a monte) non
 * possono mai trasportare uno stato tipico delle bonarie ({@code READY_TO_SEND}, {@code PROCESSING},
 * {@code SUCCESSFUL_SENDING}, {@code UNSUCCESSFUL_SENDING}).
 * <p>
 * Questa classe rende esplicita tale invariante: se uno stato bonario raggiungesse comunque il
 * boundary legale, la conversione fallisce subito con un errore chiaro e con il codice errore
 * dedicato, invece di produrre silenziosamente uno stato {@code null} a valle del mapping.
 */
public final class LegalNotificationStatusValidator {

    private LegalNotificationStatusValidator() {
    }

    /**
     * Verifica che tutte le righe del risultato di ricerca abbiano uno stato compatibile con il
     * flusso legale.
     *
     * @param serviceResult risultato paginato di ricerca da esporre sul boundary legale
     * @throws PnInternalException se almeno una riga trasporta uno stato non mappabile su
     *                             {@link NotificationStatusV26}
     */
    public static void assertLegalCompatible(ResultPaginationDto<NotificationSearchRow, String> serviceResult) {
        if (serviceResult == null || serviceResult.getResultsPage() == null) {
            return;
        }
        for (NotificationSearchRow row : serviceResult.getResultsPage()) {
            assertLegalCompatible(row.getNotificationStatus());
        }
    }

    private static void assertLegalCompatible(UnifiedNotificationStatus status) {
        if (status == null) {
            return;
        }
        try {
            NotificationStatusV26.fromValue(status.getValue());
        } catch (IllegalArgumentException ex) {
            throw new PnInternalException(
                    "Stato '" + status.getValue() + "' incompatibile con il flusso legale: "
                            + "una notifica legale non puo' assumere uno stato delle notifiche bonarie.",
                    PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INCOMPATIBLE_NOTIFICATION_STATUS,
                    ex);
        }
    }
}

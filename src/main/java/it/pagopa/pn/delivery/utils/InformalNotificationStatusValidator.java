package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.InformalNotificationStatus;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import it.pagopa.pn.delivery.models.NotificationSearchRow;
import it.pagopa.pn.delivery.models.ResultPaginationDto;

/**
 * Verifica fail-fast dell'invariante di dominio sul boundary del flusso bonario (campagna).
 * <p>
 * La riga di ricerca interna ({@link NotificationSearchRow}) trasporta lo stato unificato
 * {@link UnifiedNotificationStatus}, valido sia per le notifiche legali sia per le bonarie. Il flusso
 * bonario, per costruzione del dominio (filtro {@code communicationType = INFORMAL} applicato a
 * monte), non può mai trasportare uno stato tipico delle notifiche legali.
 * <p>
 * Questa classe rende esplicita tale invariante: se uno stato legale raggiungesse comunque il
 * boundary bonario, la conversione fallisce subito con un errore chiaro e con il codice errore
 * dedicato, invece di produrre silenziosamente uno stato {@code null} a valle del mapping
 * (ModelMapper incapsula le eccezioni del converter in {@code MappingException}, per questo la
 * verifica va eseguita fuori dal {@code map()}).
 */
public final class InformalNotificationStatusValidator {

    private InformalNotificationStatusValidator() {
    }

    /**
     * Verifica che tutte le righe del risultato di ricerca abbiano uno stato compatibile con il
     * flusso bonario.
     *
     * @param serviceResult risultato paginato di ricerca da esporre sul boundary bonario
     * @throws PnInternalException se almeno una riga trasporta uno stato non mappabile su
     *                             {@link InformalNotificationStatus}
     */
    public static void assertInformalCompatible(ResultPaginationDto<NotificationSearchRow, String> serviceResult) {
        if (serviceResult == null || serviceResult.getResultsPage() == null) {
            return;
        }
        for (NotificationSearchRow row : serviceResult.getResultsPage()) {
            assertInformalCompatible(row.getNotificationStatus());
        }
    }

    private static void assertInformalCompatible(UnifiedNotificationStatus status) {
        if (status == null) {
            return;
        }
        try {
            InformalNotificationStatus.fromValue(status.getValue());
        } catch (IllegalArgumentException ex) {
            throw new PnInternalException(
                    "Stato '" + status.getValue() + "' incompatibile con il flusso bonario: "
                            + "una notifica bonaria non puo' assumere uno stato delle notifiche legali.",
                    PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_INCOMPATIBLE_NOTIFICATION_STATUS,
                    ex);
        }
    }
}

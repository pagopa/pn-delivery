package it.pagopa.pn.delivery.models;

/**
 * Filtro per tipologia di comunicazione utilizzato dalla ricerca notifiche.
 * <ul>
 *     <li>{@code LEGAL}: solo notifiche legali (default applicativo quando il filtro è assente);</li>
 *     <li>{@code INFORMAL}: solo comunicazioni bonarie;</li>
 *     <li>{@code ALL}: entrambe le tipologie.</li>
 * </ul>
 * Concetto esclusivamente di ricerca: distinto dall'enum di dominio
 * {@link it.pagopa.pn.delivery.models.internal.notification.CommunicationType}
 * che rappresenta la tipologia effettiva di una singola notifica persistita.
 */
public enum NotificationSearchCommunicationType {
    LEGAL,
    INFORMAL,
    ALL
}

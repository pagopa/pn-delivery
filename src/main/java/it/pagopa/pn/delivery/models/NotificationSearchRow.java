package it.pagopa.pn.delivery.models;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.UnifiedNotificationStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Modello interno e generico di una riga di ricerca notifiche.
 * Contiene il super-insieme dei dati persistiti, indipendentemente dallo
 * specifico caso d'uso (ricerca per destinatario, per mittente, deleghe, ecc.).
 * La specializzazione verso i DTO generati avviene nei singoli controller.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class NotificationSearchRow {

    private String iun;

    private String paProtocolNumber;

    private String sender;

    private OffsetDateTime sentAt;

    private String subject;

    private List<String> recipients;

    private OffsetDateTime requestAcceptedAt;

    private String group;

    private String mandateId;

    // Dati statici
    private String communicationType;

    private String campaignId;

    private UnifiedNotificationStatus notificationStatus;

    // Dati dinamici (valorizzati solo per le comunicazioni bonarie)
    private Boolean viewed;

    private Boolean delivered;

    private Boolean desiredFeedback;

}

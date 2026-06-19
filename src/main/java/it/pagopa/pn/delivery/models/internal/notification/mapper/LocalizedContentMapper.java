package it.pagopa.pn.delivery.models.internal.notification.mapper;

public class LocalizedContentMapper {
    private LocalizedContentMapper() {
        // do nothing
    }

    /**
     * Converte un oggetto {@code LocalizedContent} proveniente dal client Data Vault
     * nel corrispondente DTO server-side utilizzato dall'applicazione.
     *
     * <p>Se il parametro in input è {@code null}, il metodo restituisce {@code null}.
     * I campi obbligatori del contenuto localizzato vengono copiati nel DTO target.</p>
     *
     * @param source contenuto localizzato proveniente dal client Data Vault
     * @return il contenuto localizzato convertito nel formato server-side, oppure {@code null} se il parametro in input è {@code null}
     * @throws NullPointerException se uno dei campi obbligatori del contenuto sorgente è assente
     */
    public static it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent mapToServerLocalizedContent(
            it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.LocalizedContent source) {
        if (source == null) {
            return null;
        }

        it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent target =
                new it.pagopa.pn.delivery.generated.openapi.server.v1.dto.LocalizedContent();

        target.setLanguage(source.getLanguage().getValue());
        target.setLongBody(source.getLongBody());
        target.setShortBody(source.getShortBody());
        target.setSubject(source.getSubject());

        return target;
    }
}

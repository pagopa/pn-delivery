package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.models.TaxonomyCodeDto;

import java.util.Optional;

public class TaxonomyCodeDaoDynamo implements TaxonomyCodeDao {
    @Override
    public Optional<TaxonomyCodeDto> getTaxonomyCodeByKeyAndPaId(String key, String paId) {
        return Optional.empty();
    }
}

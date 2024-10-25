package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.models.TaxonomyCodeDto;

import java.util.Optional;

public interface TaxonomyCodeDao {

    Optional<TaxonomyCodeDto> getTaxonomyCodeByKeyAndPaId(String key, String paId);

}

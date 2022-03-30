package it.pagopa.pn.delivery.middleware.directaccesstokendao;


import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.abstractions.IdConflictException;

import java.util.Optional;

public interface DirectAccessTokenDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.direct-access-token-dao";

    void addDirectAccessToken(DirectAccessToken directAccessToken) throws IdConflictException;

    Optional<DirectAccessToken> getDirectAccessToken(String token);

    void deleteByIun(String iun);
}

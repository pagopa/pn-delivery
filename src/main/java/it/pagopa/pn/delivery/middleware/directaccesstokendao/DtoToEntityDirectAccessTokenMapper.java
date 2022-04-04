package it.pagopa.pn.delivery.middleware.directaccesstokendao;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityDirectAccessTokenMapper {

    public TokenEntity dto2Entity(DirectAccessToken directAccessToken) {
        return TokenEntity.builder()
                .tokenId( directAccessToken.getToken() )
                .iun( directAccessToken.getIun() )
                .taxId( directAccessToken.getTaxId() )
                .build();
    }
}

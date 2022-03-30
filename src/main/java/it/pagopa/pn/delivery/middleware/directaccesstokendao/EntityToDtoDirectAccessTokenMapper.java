package it.pagopa.pn.delivery.middleware.directaccesstokendao;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoDirectAccessTokenMapper {

    public DirectAccessToken entity2Dto(TokenEntity tokenEntity) {
        return DirectAccessToken.builder()
                .token( tokenEntity.getToken() )
                .iun( tokenEntity.getIun() )
                .taxId( tokenEntity.getTaxId() )
                .build();
    }
}

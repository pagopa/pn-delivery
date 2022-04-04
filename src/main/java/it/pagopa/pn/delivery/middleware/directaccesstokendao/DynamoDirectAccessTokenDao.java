package it.pagopa.pn.delivery.middleware.directaccesstokendao;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

@Component
@ConditionalOnProperty( name = DirectAccessTokenDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO )
public class DynamoDirectAccessTokenDao implements DirectAccessTokenDao {
    private final DynamoDirectAccessTokenEntityDao entityDao;
    private final DtoToEntityDirectAccessTokenMapper dto2entity;
    private final EntityToDtoDirectAccessTokenMapper entity2dto;

    public DynamoDirectAccessTokenDao(DynamoDirectAccessTokenEntityDao entityDao, DtoToEntityDirectAccessTokenMapper dto2entity, EntityToDtoDirectAccessTokenMapper entity2dto) {
        this.entityDao = entityDao;
        this.dto2entity = dto2entity;
        this.entity2dto = entity2dto;
    }

    @Override
    public void addDirectAccessToken(DirectAccessToken directAccessToken) throws IdConflictException {
        TokenEntity entity = dto2entity.dto2Entity(directAccessToken);
        entityDao.putIfAbsent(entity);
    }

    @Override
    public Optional<DirectAccessToken> getDirectAccessToken(String token) {
        Key keyToSearch = Key.builder()
                .partitionValue(token)
                .build();

        return entityDao.get(keyToSearch)
                .map(entity2dto::entity2Dto);
    }

    @Override
    public void deleteByIun(String iun) {
        entityDao.findByIun(iun).forEach(entity ->{
            Key keyToDelete = Key.builder()
                    .partitionValue(entity.getTokenId())
                    .build();
            entityDao.delete(keyToDelete);
        });
    }
}

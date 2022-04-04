package it.pagopa.pn.delivery.middleware.directaccesstokendao;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        DirectAccessTokenDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
        "aws.region-code=us-east-1"
})
@SpringBootTest
class DynamoDirectAccessTokenDaoTestIT {

    @Autowired
    private DirectAccessTokenDao dao;
    @Autowired
    private DynamoDirectAccessTokenEntityDao entityDao;

    @Test
    void addAndGetDirectAccessToken() throws IdConflictException {
        //GIVEN
        String token = UUID.randomUUID().toString();
        String iun = UUID.randomUUID().toString();
        String taxId = "CGNNMO80A03H501U";

        DirectAccessToken dat = DirectAccessToken.builder()
                .token( token )
                .iun( iun )
                .taxId( taxId )
                .build();

        deleteElement(token);

        //WHEN
        dao.addDirectAccessToken(dat);

        //THEN
        Optional<DirectAccessToken> result = dao.getDirectAccessToken( token );
        
        Assertions.assertTrue( result.isPresent() );
        final DirectAccessToken directAccessToken = result.get();
        Assertions.assertNotNull( result );
        Assertions.assertEquals( token , directAccessToken.getToken() );
        Assertions.assertEquals( iun, directAccessToken.getIun() );
        Assertions.assertEquals( taxId, directAccessToken.getTaxId() );
    }

    @Test
    void assertException() {
        //GIVEN
        String iun = UUID.randomUUID().toString();
        String taxId = "CGNNMO80A03H501U";
        String token = iun + "_" + taxId;

        DirectAccessToken dat = DirectAccessToken.builder()
                .token( token )
                .iun( iun )
                .taxId( taxId )
                .build();

        deleteElement(token);
        //WHEN
        assertDoesNotThrow(() ->  dao.addDirectAccessToken(dat));

        assertThrows(IdConflictException.class, () -> {
            dao.addDirectAccessToken(dat);
        });
        
    }

    @Test
    void deleteByIun() {
        //GIVEN
        String iun = UUID.randomUUID().toString();
        String taxId = "CGNNMO80A03H501U";
        String token = iun + "_" + taxId;
        
        DirectAccessToken dat = DirectAccessToken.builder()
                .token( token )
                .iun( iun )
                .taxId( taxId )
                .build();
        deleteElement(token);

        String taxId2 = "CGNNMO80A03H501F";
        String token2 = iun + "_" + taxId2;

        DirectAccessToken dat2 = DirectAccessToken.builder()
                .token( token2 )
                .iun( iun )
                .taxId( taxId2 )
                .build();
        deleteElement(token2);

        assertDoesNotThrow(() ->  dao.addDirectAccessToken(dat));
        assertDoesNotThrow(() ->  dao.addDirectAccessToken(dat2));
        
        dao.deleteByIun(iun);

        //THEN
        Optional<DirectAccessToken> result = dao.getDirectAccessToken( token );
        Assertions.assertTrue(result.isEmpty());
    }
    
    private void deleteElement(String token) {
        Key key = Key.builder()
                .partitionValue(token)
                .build();
        entityDao.delete(key);
    }
}


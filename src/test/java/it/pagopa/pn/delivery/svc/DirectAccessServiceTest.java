package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.commons_delivery.middleware.DirectAccessTokenDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectAccessServiceTest {
    private static final String TAX_ID = "TAX_ID";
    private static final String IUN = "IUN";
    private static final String TOKEN = "TOKEN";
    private static final String SECRET = "TAX_ID-secret";
    DirectAccessTokenDao directAccessTokenDao;
    RecipientsChallenge recipientsChallenge;
    DirectAccessService directAccessService;

    @BeforeEach
    public void setup() {
        directAccessTokenDao = Mockito.mock(DirectAccessTokenDao.class);
        recipientsChallenge = Mockito.mock(RecipientsChallenge.class);

        directAccessService = new DirectAccessService(directAccessTokenDao, recipientsChallenge);

    }


    @Test
    void doChallengeSuccess() {
        //Given
        Optional<DirectAccessToken> expectedDirectAccessToken = Optional.of( DirectAccessToken.builder()
                .taxId( TAX_ID )
                .iun( IUN )
                .token( TOKEN )
                .build());

        //When
        Mockito.when( directAccessTokenDao.getDirectAccessToken( Mockito.anyString() )).thenReturn( expectedDirectAccessToken );
        Mockito.when( recipientsChallenge.getSecret( TAX_ID )).thenReturn( SECRET );
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( TOKEN , SECRET);

        //Then
        assertEquals( expectedDirectAccessToken, directAccessToken );
    }

    @Test
    void doChallengeFailure() {
        //Given
        Optional<DirectAccessToken> expectedDirectAccessToken = Optional.of( DirectAccessToken.builder()
                .taxId( TAX_ID )
                .iun( IUN )
                .token( TOKEN )
                .build());

        //When
        Mockito.when( directAccessTokenDao.getDirectAccessToken( Mockito.anyString() )).thenReturn( expectedDirectAccessToken );
        Mockito.when( recipientsChallenge.getSecret( TAX_ID )).thenReturn( "secret diverso" );
        Optional<DirectAccessToken> directAccessToken = directAccessService.doChallenge( TOKEN , SECRET);

        //Then
        assertEquals( Optional.empty(), directAccessToken );
    }
}

package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.api.dto.notification.directaccesstoken.DirectAccessToken;
import it.pagopa.pn.api.rest.PnDeliveryRestConstants;
import it.pagopa.pn.commons.pnclients.recipientschallenge.RecipientsChallenge;
import it.pagopa.pn.delivery.svc.DirectAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;

@WebFluxTest(PnDirectAccessController.class)
class PnDirectAccessControllerTest {

    public static final String TOKEN = "94815a62-6a1c-42c0-9331-5d6cabfd2309";
    public static final String TAX_ID = "CGNNMO80A01H501M";
    public static final String USER_SECRET = TAX_ID + "-secret";
    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private DirectAccessService svc;

    @MockBean
    private RecipientsChallenge recipientsChallenge;

    @Test
    void getSuccess() {
        //Given
        DirectAccessToken directAccessToken = DirectAccessToken.builder()
                .token(TOKEN)
                .iun("202111-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .taxId(TAX_ID)
                .build();

        //When
        Mockito.when(svc.doChallenge( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn(Optional.of(directAccessToken));

        Mockito.when(recipientsChallenge.getSecret( TAX_ID ))
                        .thenReturn( USER_SECRET );

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.DIRECT_ACCESS_PATH )
                                .queryParam("token",TOKEN)
                                .build())
                .accept(MediaType.ALL)
                .header("user_secret", USER_SECRET)
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(svc).doChallenge(TOKEN, USER_SECRET);
    }

    @Test
    void getUnauthorized() {
        //Given
        DirectAccessToken directAccessToken = DirectAccessToken.builder()
                .token(TOKEN)
                .iun("202111-2d74ffe9-aa40-47c2-88ea-9fb171ada637")
                .taxId(TAX_ID)
                .build();

        //When
        Mockito.when(svc.doChallenge( Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( Optional.empty() );

        //Then
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path( "/" + PnDeliveryRestConstants.DIRECT_ACCESS_PATH )
                                .queryParam("token",TOKEN)
                                .build())
                .accept(MediaType.ALL)
                .header("user_secret", USER_SECRET)
                .exchange()
                .expectStatus()
                .isUnauthorized();

        Mockito.verify(svc).doChallenge(TOKEN, USER_SECRET);
    }
}

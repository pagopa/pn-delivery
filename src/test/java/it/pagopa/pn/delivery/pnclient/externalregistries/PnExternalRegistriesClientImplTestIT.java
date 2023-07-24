package it.pagopa.pn.delivery.pnclient.externalregistries;

import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentInfo;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaymentStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.external-registries-base-url=http://localhost:9998",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
class PnExternalRegistriesClientImplTestIT {

    private static final String PA_TAX_ID = "paTaxId";
    private static final String NOTICE_CODE = "noticeCode";
    private static final String SENDER_ID = "senderId";

    @Autowired
    private PnExternalRegistriesClientImpl externalRegistriesClient;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {

        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    @Test
    void getPaymentInfo() {

        String path = "/ext-registry/pagopa/v1/paymentinfo/paTaxId/noticeCode";

        PaymentInfo paymentInfo = new PaymentInfo()
                .amount( 12000 )
                .status( PaymentStatus.REQUIRED )
                .url( "https://api.uat.platform.pagopa.it/checkout/auth/payments/v2" );

        // When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );
        externalRegistriesClient.getPaymentInfo( PA_TAX_ID, NOTICE_CODE );

        // Then
        assertDoesNotThrow( () -> {
            externalRegistriesClient.getPaymentInfo( PA_TAX_ID, NOTICE_CODE );
        });
    }

    @Test
    void getGroupsSuccess() {

        String path = "/ext-registry-private/pa/v1/groups-all";

        List<PaGroup> res = new ArrayList<>();
        PaGroup dto = new PaGroup();
        dto.setId("123456789");
        dto.setName("amministrazione");
        res.add(dto);
        dto = new PaGroup();
        dto.setId("987654321");
        dto.setName("dirigenza");
        res.add(dto);

        // When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        externalRegistriesClient.getGroups( SENDER_ID , false);

        // Then
        assertDoesNotThrow( () -> {
            externalRegistriesClient.getGroups( SENDER_ID, false );
        });
    }


    @Test
    void getGroupsSuccess_activeonly() {

        String path = "/ext-registry-private/pa/v1/groups-all";

        List<PaGroup> res = new ArrayList<>();
        PaGroup dto = new PaGroup();
        dto.setId("123456789");
        dto.setName("amministrazione");
        res.add(dto);
        dto = new PaGroup();
        dto.setId("987654321");
        dto.setName("dirigenza");
        res.add(dto);

        // When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("statusFilter", "ACTIVE")
                )
                .respond(response()
                        .withStatusCode(200)
                );


        // Then
        assertDoesNotThrow( () -> {
            externalRegistriesClient.getGroups( SENDER_ID, true );
        });
    }

}

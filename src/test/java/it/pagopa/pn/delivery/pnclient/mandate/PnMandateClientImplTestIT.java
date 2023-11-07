package it.pagopa.pn.delivery.pnclient.mandate;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.MockAWSObjectsTest;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.model.InternalMandateDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.mandate-base-url=http://localhost:9998",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
})
class PnMandateClientImplTestIT extends MockAWSObjectsTest {

    private static final String DELEGATE = "delegate";
    private static final String DELEGATOR = "delegator";
    private static final String MANDATE_ID = "mandate_id";
    private static final String MANDATE_DATE_FROM = "2022-04-22T16:00:00Z";
    @Autowired
    private PnMandateClientImpl mandateClient;

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
    void getMandatesSuccess() {
        //Given
        String path = "/mandate-private/api/v1/mandates-by-internaldelegate/delegate";

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE);
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);

        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        //Then
        assertDoesNotThrow( () -> {
            mandateClient.listMandatesByDelegate(DELEGATE, MANDATE_ID, CxTypeAuthFleet.PF, null);
        });

    }


    @Test
    void getMandatesFailTimeout() {
        //Given
        String path = "/mandate-private/api/v1/mandates-by-internaldelegate/delegatea";

        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE+"a");
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);

        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                        .withDelay(Delay.milliseconds(10000))
                );

        //Then
        assertThrows(ResourceAccessException.class, () -> {
            mandateClient.listMandatesByDelegate(DELEGATE+"a", MANDATE_ID, CxTypeAuthFleet.PF, null);
        });

    }

    @Test
    void getMandatesByDelegatorSuccess() {
        //Given
        String path = "/mandate-private/api/v1/mandates-by-internaldelegator/delegate";
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE);
        internalMandateDto.setDelegator(DELEGATOR);
        internalMandateDto.mandateId(MANDATE_ID);
        internalMandateDto.setDatefrom(MANDATE_DATE_FROM);

        //When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        //Then
        assertDoesNotThrow( () -> {
            mandateClient.listMandatesByDelegator(DELEGATE, MANDATE_ID, CxTypeAuthFleet.PF, null, null, null);
        });
    }

}

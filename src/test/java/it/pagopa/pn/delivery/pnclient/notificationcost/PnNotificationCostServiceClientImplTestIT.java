package it.pagopa.pn.delivery.pnclient.notificationcost;

import it.pagopa.pn.delivery.MockAWSObjectsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.notification-cost-service-base-url=http://localhost:9998",
})
public class PnNotificationCostServiceClientImplTestIT extends MockAWSObjectsTest {

    private static final String IUV = "test-iuv";

    @Autowired
    private PnNotificationCostServiceClientImpl pnNotificationCostServiceClient;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void getNotificationCostRecipientSuccess() {
        // Given
        String path = "/notification-cost-private/cost/payment/" + IUV;

        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath(path)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"iun\":\"test-iun\",\"recIndex\":1,\"cost\":0.05}")
        );

        assertDoesNotThrow(() -> pnNotificationCostServiceClient.getNotificationCostByPayment(IUV));
    }
}

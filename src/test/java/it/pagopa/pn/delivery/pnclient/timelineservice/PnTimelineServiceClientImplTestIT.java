package it.pagopa.pn.delivery.pnclient.timelineservice;

import it.pagopa.pn.delivery.MockAWSObjectsTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.timeline-service-base-url=http://localhost:9998",
})
public class PnTimelineServiceClientImplTestIT extends MockAWSObjectsTest {

    private static final String IUN = "test-iun";
    private static final Integer REC_INDEX = 1;

    @Autowired
    private PnTimelineServiceClientImpl pnTimelineServiceClientImpl;

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
    void getDeliveryInformation_shouldReturnResponse() {
        // Given
        String responseBody = """
        {
          "deliveryMode": "DIGITAL",
          "isNotificationCancelled": false,
          "isNotificationAccepted": true
        }
        """;
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath("/timeline-service-private/delivery-information/" + IUN)
                        .withQueryStringParameter("recIndex", REC_INDEX.toString())
                ).respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                );

        // When
        var result = pnTimelineServiceClientImpl.getDeliveryInformation(IUN, REC_INDEX);

        // Then
        assertNotNull(result);
        assertEquals("DIGITAL", result.getDeliveryMode().toString());
        assertFalse(result.getIsNotificationCancelled());
        assertTrue(result.getIsNotificationAccepted());
    }
}

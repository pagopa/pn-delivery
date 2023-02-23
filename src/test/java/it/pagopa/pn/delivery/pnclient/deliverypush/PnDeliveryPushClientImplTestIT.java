package it.pagopa.pn.delivery.pnclient.deliverypush;

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

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.delivery-push-base-url=http://localhost:9998",
})
class PnDeliveryPushClientImplTestIT extends MockAWSObjectsTest {


    @Autowired
    private PnDeliveryPushClientImpl deliveryPushClient;


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
    void getTimelineAndStatusHistory() {
        String path = "/delivery-push-private/DHUJ-QYVT-DMVH-202302-P-1/history";
        OffsetDateTime createdAt = OffsetDateTime.parse("2023-02-22T10:11:12.123Z");
        // Given

        // When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("numberOfRecipients", "2")
                        .withQueryStringParameter("createdAt", "2023-02-22T10:11:12.123Z")
                )
                .respond(response()
                        .withStatusCode(200)
                );

        deliveryPushClient.getTimelineAndStatusHistory( "DHUJ-QYVT-DMVH-202302-P-1", 2, createdAt );
        // Then
        assertDoesNotThrow( () -> {
            deliveryPushClient.getTimelineAndStatusHistory( "DHUJ-QYVT-DMVH-202302-P-1", 2, createdAt );
        });
    }

}

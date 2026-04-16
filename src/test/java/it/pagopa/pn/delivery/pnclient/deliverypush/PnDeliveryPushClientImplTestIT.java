package it.pagopa.pn.delivery.pnclient.deliverypush;

import it.pagopa.pn.delivery.MockAWSObjectsTest;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationCancelledException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationFeePolicy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;

import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;
import static it.pagopa.pn.delivery.svc.NotificationPriceService.ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.delivery.delivery-push-base-url=http://localhost:9998",
        "pn.delivery.max-recipients-count=0",
        "pn.delivery.max-attachments-count=0"
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

    @AfterEach
    public void resetMockServer() {
        if (mockServer != null) {
            mockServer.reset();
        }
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

    @Test
    void getNotificationProcessCost() {
        String path = "/delivery-push-private/DHUJ-QYVT-DMVH-202302-P-1/notification-process-cost/0";
        // Given

        // When
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter("notificationFeePolicy", NotificationFeePolicy.FLAT_RATE.getValue())
                )
                .respond(response()
                        .withStatusCode(200)
                );

        deliveryPushClient.getNotificationProcessCost( "DHUJ-QYVT-DMVH-202302-P-1", 0, NotificationFeePolicy.FLAT_RATE ,false, 0, 22);
        // Then
        assertDoesNotThrow( () -> {
            deliveryPushClient.getNotificationProcessCost( "DHUJ-QYVT-DMVH-202302-P-1", 0, NotificationFeePolicy.FLAT_RATE, false, 0, 22 );
        });
    }

    @Test
    void getNotificationProcessCost_notificationCancelled() {
        String path = "/delivery-push-private/DHUJ-QYVT-DMVH-202302-P-1/notification-process-cost/0";

        String responseBody = """
        {
          "type": "about:blank",
          "title": "Not Found",
          "status": 404,
          "errors": [
            {
              "code": "%s"
            }
          ]
        }
        """.formatted(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter(
                                "notificationFeePolicy",
                                NotificationFeePolicy.FLAT_RATE.getValue()
                        )
                )
                .respond(response()
                        .withStatusCode(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                );

        assertThrows(PnNotificationCancelledException.class, () ->
                deliveryPushClient.getNotificationProcessCost(
                        "DHUJ-QYVT-DMVH-202302-P-1",
                        0,
                        NotificationFeePolicy.FLAT_RATE,
                        false,
                        0,
                        22
                )
        );
    }

    @Test
    void getNotificationProcessCost_notificationNotAccepted() {
        String path = "/delivery-push-private/DHUJ-QYVT-DMVH-202302-P-1/notification-process-cost/0";

        String responseBody = """
        {
          "type": "about:blank",
          "title": "Not Found",
          "status": 404,
          "errors": [
            {
              "code": "%s"
            }
          ]
        }
        """.formatted(ERROR_CODE_DELIVERY_PUSH_NOTIFICATION_NOT_ACCEPTED);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path)
                        .withQueryStringParameter(
                                "notificationFeePolicy",
                                NotificationFeePolicy.FLAT_RATE.getValue()
                        )
                )
                .respond(response()
                        .withStatusCode(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                );

        assertThrows(PnNotFoundException.class, () ->
                deliveryPushClient.getNotificationProcessCost(
                        "DHUJ-QYVT-DMVH-202302-P-1",
                        0,
                        NotificationFeePolicy.FLAT_RATE,
                        false,
                        0,
                        22
                )
        );
    }
}

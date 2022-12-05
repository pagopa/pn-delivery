package it.pagopa.pn.delivery.rest.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.ModelMapperFactory;
import it.pagopa.pn.delivery.utils.io.IOMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(controllers = {PnReceivedIONotificationsController.class})
class PnReceivedIONotificationsControllerTest {

    private static final String IUN = "IUN";
    private static final String USER_ID = "USER_ID";
    private static final String PA_ID = "PA_ID";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService svc;

    @SpyBean
    private IOMapper ioMapper;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    ModelMapperFactory modelMapperFactory;

    @Test
    void getReceivedNotificationSuccess() {
        // Given
        InternalNotification notification = newNotification();
        String expectedValueJson = newThirdPartyMessage(notification);
        System.out.println(expectedValueJson);

        // When
        Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.anyString(), eq( null ) ) )
                .thenReturn( notification );


        // Then
        webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN  )
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-cx-taxid", USER_ID )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(expectedValueJson);

        Mockito.verify( svc ).getNotificationAndNotifyViewedEvent(IUN, USER_ID, null);
    }

    @Test
    void getReceivedNotificationFailure() {

        // When
        Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.anyString(), eq( null ) ) )
                .thenThrow(new PnNotificationNotFoundException("test"));

        // Then
        webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN  )
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-cx-taxid", "asdasd" )
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    private InternalNotification newNotification() {
        return new InternalNotification(FullSentNotification.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .senderPaId( PA_ID )
                ._abstract("Abstract")
                .notificationStatus( NotificationStatus.ACCEPTED )
                .recipients( Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("Codice Fiscale 01")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(NotificationDigitalAddress.builder()
                                        .type( NotificationDigitalAddress.TypeEnum.PEC )
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .documents(Arrays.asList(
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc00")
                                        .build()
                                )
                                .build(),
                        NotificationDocument.builder()
                                .ref( NotificationAttachmentBodyRef.builder()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                        .build()
                                )
                                .digests(NotificationAttachmentDigests.builder()
                                        .sha256("sha256_doc01")
                                        .build()
                                )
                                .build()
                ))
                .timeline( Collections.singletonList(TimelineElement.builder().build()))
                .notificationStatusHistory( Collections.singletonList( NotificationStatusHistoryElement.builder()
                        .status( NotificationStatus.ACCEPTED )
                        .build() ) )
                .build(), Collections.emptyList());
    }

    private String newThirdPartyMessage(InternalNotification notification) {
        try {
            ThirdPartyMessage thirdPartMessage = ioMapper.mapToThirdPartMessage(notification);
            return objectMapper.writeValueAsString(thirdPartMessage);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

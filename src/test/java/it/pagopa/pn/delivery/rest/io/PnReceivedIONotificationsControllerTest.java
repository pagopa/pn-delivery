package it.pagopa.pn.delivery.rest.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.exception.PnIoMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnMandateNotFoundException;
import it.pagopa.pn.delivery.exception.PnNotificationNotFoundException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.RequestCheckQrMandateDto;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ResponseCheckQrMandateDto;
import it.pagopa.pn.delivery.exception.PnRootIdNonFountException;
import it.pagopa.pn.delivery.generated.openapi.server.appio.v1.dto.ThirdPartyMessage;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationDigitalAddress;
import it.pagopa.pn.delivery.models.InternalAuthHeader;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.internal.notification.*;
import it.pagopa.pn.delivery.models.internal.notification.F24Payment;
import it.pagopa.pn.delivery.models.internal.notification.NotificationAttachmentBodyRef;
import it.pagopa.pn.delivery.models.internal.notification.NotificationDocument;
import it.pagopa.pn.delivery.models.internal.notification.PagoPaPayment;
import it.pagopa.pn.delivery.svc.NotificationQRService;
import it.pagopa.pn.delivery.svc.search.NotificationRetrieverService;
import it.pagopa.pn.delivery.utils.io.IOMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(controllers = {PnReceivedIONotificationsController.class})
class PnReceivedIONotificationsControllerTest {

    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
    private static final String USER_ID = "USER_ID";
    private static final String PA_ID = "PA_ID";
    private static final String X_PAGOPA_PN_SRC_CH = "sourceChannel";
    private static final String X_PAGOPA_PN_SRC_CH_DET = "sourceChannelDetails";
    private static final String MANDATE_ID = "3766e7f2-70b2-4264-8ca8-4548fe728b0d";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationRetrieverService svc;

    @MockBean
    private NotificationQRService notificationQRService;

    @SpyBean
    private IOMapper ioMapper;

    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    ModelMapper modelMapper;

    @Test
    void getReceivedNotificationSuccess() {
        // Given
        InternalNotification notification = newNotification();
        String expectedValueJson = newThirdPartyMessage(notification, false);
        System.out.println(expectedValueJson);

        // When
        Mockito.when( svc.getNotificationAndNotifyViewedEvent( Mockito.anyString(), Mockito.any( InternalAuthHeader.class ), eq( null)) )
                .thenReturn( notification );

        // Then
        webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN  )
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(expectedValueJson);

        Mockito.verify(svc).getNotificationAndNotifyViewedEvent(IUN, new InternalAuthHeader("PF", "IO-" + USER_ID, USER_ID, null, X_PAGOPA_PN_SRC_CH, X_PAGOPA_PN_SRC_CH_DET), null);
    }

    @Test
    void getReceivedNotificationFailure() {

        // When
        Mockito.when(svc.getNotificationAndNotifyViewedEvent(Mockito.anyString(), Mockito.any(InternalAuthHeader.class), eq(null)))
                .thenThrow(new PnNotificationNotFoundException("test"));

        // Then
        webTestClient.get()
                .uri( "/delivery/notifications/received/" + IUN  )
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getReceivedNotificationWithMandateFailsForRootId() {

        // When
        Mockito.when(svc.getNotificationAndNotifyViewedEvent(Mockito.anyString(), Mockito.any(InternalAuthHeader.class), eq(MANDATE_ID)))
                .thenThrow(new PnRootIdNonFountException("test"));

        // Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/delivery/notifications/received/" + IUN)
                        .queryParam("mandateId", MANDATE_ID)
                        .build())
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getReceivedNotificationWithMandateFailsForMandate() {

        // When
        Mockito.when(svc.getNotificationAndNotifyViewedEvent(Mockito.anyString(), Mockito.any(InternalAuthHeader.class), eq(MANDATE_ID)))
                .thenThrow(new PnMandateNotFoundException("test"));

        // Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/delivery/notifications/received/" + IUN)
                        .queryParam("mandateId", MANDATE_ID)
                        .build())
                .header(HttpHeaders.ACCEPT, "application/io+json")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .attribute("mandateId", "MANDATE_ID")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }


    @Test
    void checkAarQrCodeIOSuccess() {
        // Given
        RequestCheckQrMandateDto requestCheckQrMandateDto = new RequestCheckQrMandateDto();
        requestCheckQrMandateDto.setAarQrCodeValue("aarQrCodeValue");
        ResponseCheckQrMandateDto responseCheckQrMandateDto = new ResponseCheckQrMandateDto();
        responseCheckQrMandateDto.setIun(IUN);

        // When
        Mockito.when(notificationQRService.getNotificationByQRFromIOWithMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(responseCheckQrMandateDto);

        // Then
        webTestClient.post()
                .uri("/delivery/notifications/received/check-qr-code")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-cx-taxid", "FAKE_TAX_ID")
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .body(Mono.just(requestCheckQrMandateDto), RequestCheckQrMandateDto.class)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }

    @Test
    void checkAarQrCodeIOFailsForMandateNotFound() {
        // Given
        RequestCheckQrMandateDto requestCheckQrMandateDto = new RequestCheckQrMandateDto();
        requestCheckQrMandateDto.setAarQrCodeValue("aarQrCodeValue");
        ResponseCheckQrMandateDto responseCheckQrMandateDto = new ResponseCheckQrMandateDto();
        responseCheckQrMandateDto.setIun(IUN);

        // When
        Mockito.when(notificationQRService.getNotificationByQRFromIOWithMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new PnIoMandateNotFoundException(responseCheckQrMandateDto));

        // Then
        webTestClient.post()
                .uri("/delivery/notifications/received/check-qr-code")
                .header("x-pagopa-pn-cx-id", "IO-" +USER_ID )
                .header("x-pagopa-cx-taxid", "FAKE_TAX_ID")
                .header("x-pagopa-pn-cx-type", "PF" )
                .header("x-pagopa-pn-uid", USER_ID )
                .header("x-pagopa-pn-src-ch", X_PAGOPA_PN_SRC_CH)
                .header("x-pagopa-pn-src-ch-details", X_PAGOPA_PN_SRC_CH_DET)
                .body(Mono.just(requestCheckQrMandateDto), RequestCheckQrMandateDto.class)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    private InternalNotification newNotification() {
        TimelineElementV27 timelineElement = new TimelineElementV27();
        timelineElement.setCategory(TimelineElementCategoryV27.AAR_CREATION_REQUEST);
        InternalNotification internalNotification = new InternalNotification();
        internalNotification.setNotificationStatusHistory(List.of(NotificationStatusHistoryElementV26.builder()
                .status(NotificationStatusV26.ACCEPTED).build()));
        internalNotification.setPagoPaIntMode(NewNotificationRequestV25.PagoPaIntModeEnum.NONE);
        internalNotification.setTimeline(List.of(timelineElement));
        internalNotification.setIun("iun");
        internalNotification.setPaProtocolNumber("protocol_01");
        internalNotification.setSubject("Subject 01");
        internalNotification.setCancelledIun("IUN_05");
        internalNotification.setCancelledIun("IUN_00");
        internalNotification.setSenderPaId(PA_ID);
        internalNotification.setNotificationStatus(NotificationStatusV26.ACCEPTED);
        internalNotification.setSourceChannel(X_PAGOPA_PN_SRC_CH);
        internalNotification.setDocuments(List.of(NotificationDocument.builder()
                .title("title")
                .contentType("application/pdf")
                .ref(NotificationAttachmentBodyRef.builder()
                        .key("ssKey")
                        .versionToken("versionToken").build())
                .docIdx("docIdx").build()));
        internalNotification.setRecipients(Collections.singletonList(
                NotificationRecipient.builder()
                        .taxId("Codice Fiscale 01")
                        .recipientType(NotificationRecipientV24.RecipientTypeEnum.PF)
                        .denomination("Nome Cognome/Ragione Sociale")
                        .internalId( "recipientInternalId" )
                        .payments(List.of(NotificationPaymentInfo.builder()
                                        .f24(F24Payment.builder()
                                                .metadataAttachment(MetadataAttachment.builder()
                                                        .ref(NotificationAttachmentBodyRef.builder()
                                                                .key("ssKey").build())
                                                        .build())
                                                .build())
                                .pagoPa(PagoPaPayment.builder().build()).build()))
                        .digitalDomicile(it.pagopa.pn.delivery.models.internal.notification.NotificationDigitalAddress.builder()
                                .type( NotificationDigitalAddress.TypeEnum.PEC )
                                .address("account@dominio.it")
                                .build()).build()));
        return internalNotification;
    }

    private String newThirdPartyMessage(InternalNotification notification, boolean isCancelled) {
        try {
            ThirdPartyMessage thirdPartMessage = ioMapper.mapToThirdPartMessage(notification, isCancelled);
            return objectMapper.writeValueAsString(thirdPartMessage);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

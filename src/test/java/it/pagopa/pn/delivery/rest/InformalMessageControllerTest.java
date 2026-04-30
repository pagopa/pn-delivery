package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.svc.InformalMessageService;
import it.pagopa.pn.delivery.utils.PnDeliveryRestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = InformalMessageController.class)
class InformalMessageControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private InformalMessageService informalMessageService;

    @Test
    void testGetMessageById() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(messageId);
        when(informalMessageService.getInformalMessageById(messageId, senderId)).thenReturn(responseDto);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/delivery/v1/messages/{messageId}").build(messageId))
                .header(PnDeliveryRestConstants.CX_ID_HEADER, senderId.toString())
                .header(PnDeliveryRestConstants.UID_HEADER, "uid-test")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group1")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testNewMessage() {
        UUID senderId = UUID.randomUUID();
        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(UUID.randomUUID());
        when(informalMessageService.createInformalMessage(any(NewMessageRequest.class), any(String.class))).thenReturn(responseDto);

        String validBody = "{" +
                "\"primaryMessage\": {" +
                "  \"subject\": \"Oggetto di test\"," +
                "  \"longBody\": \"Corpo lungo di test\"," +
                "  \"shortBody\": \"Breve corpo\"," +
                "  \"language\": \"IT\"" +
                "}}";
        webTestClient.post()
                .uri("/delivery/v1/messages")
                .header(PnDeliveryRestConstants.CX_ID_HEADER, senderId.toString())
                .header(PnDeliveryRestConstants.UID_HEADER, "uid-test")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group1")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validBody)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void testNewMessage_BadRequest() {
        UUID senderId = UUID.randomUUID();
        when(informalMessageService.createInformalMessage(any(NewMessageRequest.class), any(String.class)))
                .thenThrow(new PnBadRequestException("Primary message language must be IT", "Primary message language must be IT", "PRIMARY_LANGUAGE_NOT_IT"));

        webTestClient.post()
                .uri("/delivery/v1/messages")
                .header(PnDeliveryRestConstants.CX_ID_HEADER, senderId.toString())
                .header(PnDeliveryRestConstants.UID_HEADER, "uid-test")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group1")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testGetMessageById_NotFound() {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        when(informalMessageService.getInformalMessageById(messageId, senderId)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Message not found"));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/delivery/v1/messages/{messageId}").build(messageId))
                .header(PnDeliveryRestConstants.CX_ID_HEADER, senderId.toString())
                .header(PnDeliveryRestConstants.UID_HEADER, "uid-test")
                .header(PnDeliveryRestConstants.CX_TYPE_HEADER, "PF")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group1")
                .header(PnDeliveryRestConstants.CX_GROUPS_HEADER, "Group2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }
}

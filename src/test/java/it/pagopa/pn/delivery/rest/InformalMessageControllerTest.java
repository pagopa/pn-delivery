package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.svc.InformalMessageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(InformalMessageController.class)
class InformalMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InformalMessageService informalMessageService;

    @Test
    void testGetMessageById() throws Exception {
        UUID messageId = UUID.randomUUID();
        String senderId = UUID.randomUUID().toString();
        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(messageId);
        when(informalMessageService.getInformalMessageById(Mockito.eq(messageId), Mockito.any(UUID.class))).thenReturn(responseDto);

        mockMvc.perform(MockMvcRequestBuilders.get("/delivery/v1/messages/{messageId}", messageId)
                .param("xPagopaPnCxId", senderId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testNewMessage() throws Exception {
        String senderId = UUID.randomUUID().toString();
        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(UUID.randomUUID());
        when(informalMessageService.createInformalMessage(any(MessageRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/delivery/v1/messages")
                .param("xPagopaPnCxId", senderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void testNewMessage_BadRequest() throws Exception {
        String senderId = UUID.randomUUID().toString();
        when(informalMessageService.createInformalMessage(any(MessageRequestDto.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Primary message language must be IT"));

        mockMvc.perform(MockMvcRequestBuilders.post("/delivery/v1/messages")
                .param("xPagopaPnCxId", senderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void testGetMessageById_NotFound() throws Exception {
        UUID messageId = UUID.randomUUID();
        String senderId = UUID.randomUUID().toString();
        when(informalMessageService.getInformalMessageById(Mockito.eq(messageId), Mockito.any(UUID.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Message not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/delivery/v1/messages/{messageId}", messageId)
                .param("xPagopaPnCxId", senderId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}

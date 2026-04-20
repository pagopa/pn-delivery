package it.pagopa.pn.delivery.rest;

import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.api.MessagesApi;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.MessageResponse;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.rest.mapper.InformalMessageMapper;
import it.pagopa.pn.delivery.svc.InformalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class InformalMessageController implements MessagesApi {
    private final InformalMessageService informalMessageService;

    public InformalMessageController(InformalMessageService informalMessageService) {
        this.informalMessageService = informalMessageService;
    }

    @Override
    public ResponseEntity<MessageResponse> getMessageById(
            UUID messageId,
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            List<String> xPagopaPnCxGroups
    ) {
        MessageResponseDto dto = informalMessageService.getInformalMessageById(messageId, UUID.fromString(xPagopaPnCxId));
        return ResponseEntity.ok(InformalMessageMapper.toApi(dto));
    }

    @Override
    public ResponseEntity<MessageResponse> newMessage(
            String xPagopaPnUid,
            CxTypeAuthFleet xPagopaPnCxType,
            String xPagopaPnCxId,
            NewMessageRequest newMessageRequest,
            List<String> xPagopaPnCxGroups
    ) {
        MessageRequestDto dto = InformalMessageMapper.toMs(newMessageRequest, xPagopaPnCxId);
        MessageResponseDto result = informalMessageService.createInformalMessage(dto);
        return ResponseEntity.status(201).body(InformalMessageMapper.toApi(result));
    }
}

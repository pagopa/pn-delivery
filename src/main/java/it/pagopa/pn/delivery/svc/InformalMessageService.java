package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.svc.validation.InformalMessageValidator;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewMessageRequest;
import it.pagopa.pn.delivery.rest.mapper.InformalMessageMapper;
import it.pagopa.pn.delivery.pnclient.datavault.PnDataVaultClientImpl;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor

public class InformalMessageService {
    private final PnDataVaultClientImpl pnDataVaultClient;
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public MessageResponseDto createInformalMessage(NewMessageRequest newMessageRequest, String senderId) {
        InformalMessageValidator.validate(newMessageRequest, pnDeliveryConfigs);
        MessageRequestDto requestDto = InformalMessageMapper.toMs(newMessageRequest, senderId);
        return pnDataVaultClient.createInformalMessage(requestDto);
    }


    public MessageResponseDto getInformalMessageById(UUID messageId, UUID senderId) {
        return pnDataVaultClient.getInformalMessageById(messageId, senderId);
    }
}

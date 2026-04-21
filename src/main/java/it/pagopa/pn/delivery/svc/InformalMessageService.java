package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.svc.validation.InformalMessageValidator;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageRequestDto;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.model.MessageResponseDto;
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

    public MessageResponseDto createInformalMessage(MessageRequestDto requestDto) {
        InformalMessageValidator.validate(requestDto, pnDeliveryConfigs);
        return pnDataVaultClient.createInformalMessage(requestDto);
    }

    // ...existing code...

    public MessageResponseDto getInformalMessageById(UUID messageId, UUID senderId) {
        return pnDataVaultClient.getInformalMessageById(messageId, senderId);
    }
}

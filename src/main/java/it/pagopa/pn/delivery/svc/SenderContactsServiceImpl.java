package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnNotFoundException;
import it.pagopa.pn.delivery.models.SenderContactsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.ERROR_CODE_DELIVERY_SENDER_CONTACTS_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenderContactsServiceImpl implements SenderContactsService {
    private final PnDeliveryConfigs cfg;

    @Override
    public SenderContactsDto getSenderContacts(@Nonnull String senderId) {
        List<SenderContactsDto> senderContactsList = cfg.getSenderContacts();
        log.debug("Looking for contacts for senderId: {} in list of size: {}", senderId, senderContactsList.size());

        for(SenderContactsDto senderContacts : senderContactsList) {
            if(senderId.equals(senderContacts.getSenderId())) {
                return senderContacts;
            }
        }

        String description = String.format("No contacts found for senderId: %s", senderId);
        throw new PnNotFoundException("Contacts not found", description, ERROR_CODE_DELIVERY_SENDER_CONTACTS_NOT_FOUND);
    }
}
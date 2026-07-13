package it.pagopa.pn.delivery.svc;


import it.pagopa.pn.delivery.models.SenderContactsDto;

public interface SenderContactsService {
    SenderContactsDto getSenderContacts(String senderId);
}

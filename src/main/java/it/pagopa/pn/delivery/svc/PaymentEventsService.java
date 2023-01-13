package it.pagopa.pn.delivery.svc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentEventsService {



    // per ogni evento di pagamento nella lista di eventi

    // se presenti creditorTaxId e noticeNumber
    // chiamo private final NotificationCostEntityDao notificationCostEntityDao; per recuperare iun, recipientIdx e recipientType


    // se non presenti creditorTaxId e noticeNumber
    // chiamo NotificationRetrieverService getInternalNotification( iun )
    // trovo il recipientIdx del destinatario (forse devo opacizzare il cf del destinatario per matching)
    // creo evento
    // pubblico evento sulla coda di delivery-push
}
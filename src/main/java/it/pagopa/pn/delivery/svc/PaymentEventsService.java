package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.PaymentEventsRequest;
import it.pagopa.pn.delivery.middleware.PaymentEventsProducer;
import it.pagopa.pn.delivery.models.InternalPaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PaymentEventsService {

    private final PaymentEventsProducer paymentEventsProducer;

    public PaymentEventsService(PaymentEventsProducer paymentEventsProducer) {
        this.paymentEventsProducer = paymentEventsProducer;
    }

    public void handlePaymentEvents(String cxType, String xPagopaPnCxId, String xPagopaPnUid, PaymentEventsRequest paymentEventsRequest) {

        List<InternalPaymentEvent> paymentEvents = new ArrayList<>( paymentEventsRequest.getEvents().size() );
        paymentEventsProducer.sendPaymentEvents( paymentEvents );
    }

    // per ogni evento di pagamento nella lista di eventi

    // se presenti creditorTaxId e noticeNumber
    // chiamo private final NotificationCostEntityDao notificationCostEntityDao; per recuperare iun, recipientIdx e recipientType


    // se non presenti creditorTaxId e noticeNumber
    // chiamo NotificationRetrieverService getInternalNotification( iun )
    // trovo il recipientIdx del destinatario (forse devo opacizzare il cf del destinatario per matching)
    // creo evento
    // pubblico evento sulla coda di delivery-push
}
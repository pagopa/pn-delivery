package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.BaseCostComponent;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.BaseCostName;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostPaymentResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import org.springframework.stereotype.Component;

@Component
public class NotificationProcessCostResponseMapper {
    public NotificationProcessCostResponseInt fromExternal(NotificationProcessCostResponse ext) {
        if (ext == null) throw new PnInternalException("Cannot map null NotificationProcessCostResponse", "PN_GENERIC_ERROR");
        NotificationProcessCostResponseInt internal = new NotificationProcessCostResponseInt();
        internal.setPartialCost(ext.getPartialCost());
        internal.setTotalCost(ext.getTotalCost());
        internal.setAnalogCost(ext.getAnalogCost());
        internal.setPaFee(ext.getPaFee());
        internal.setSendFee(ext.getSendFee());
        internal.setVat(ext.getVat());
        internal.setRefinementDate(ext.getRefinementDate());
        internal.setNotificationViewDate(ext.getNotificationViewDate());
        return internal;
    }

    public NotificationProcessCostResponseInt mapFromTimelineAndCostResponse(DeliveryInformationResponse timelineResponse, NotificationCostPaymentResponse costResponse) {
        if (timelineResponse == null || costResponse == null) throw new PnInternalException("Cannot process notification data: required response is null", "PN_GENERIC_ERROR");
        NotificationProcessCostResponseInt internal = new NotificationProcessCostResponseInt();
        internal.setPartialCost(costResponse.getPartialCost().getCost());
        internal.setTotalCost(costResponse.getTotalCost().getCostWithVat());
        internal.setAnalogCost(getAnalogCost(costResponse));
        internal.setPaFee(getBaseCostComponent(BaseCostName.PA_FEE, costResponse));
        internal.setSendFee(getBaseCostComponent(BaseCostName.SEND_FEE, costResponse));
        internal.setVat(getVat(costResponse));
        if (timelineResponse.getRefinementOrViewedDateDetail() != null) {
            internal.setRefinementDate(timelineResponse.getRefinementOrViewedDateDetail().getRefinementDate());
            internal.setNotificationViewDate(timelineResponse.getRefinementOrViewedDateDetail().getViewedDate());
        }
        return internal;
    }

    private int getBaseCostComponent(BaseCostName costName, NotificationCostPaymentResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getBaseCostDetail().getBaseCostComponents().stream()
                .filter(baseCostComponent -> baseCostComponent.getCostName().equals(costName))
                .mapToInt(BaseCostComponent::getCost)
                .findFirst()
                .orElseThrow(() -> new PnInternalException(
                        String.format("BaseCostComponent %s non trovato o struttura costResponse non valida", costName),"PN_GENERIC_ERROR")
                );
    }

    private int getAnalogCost(NotificationCostPaymentResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getAnalogCostDetail() != null ? costResponse.getTotalCost().getDetails().getAnalogCostDetail().getCost() : 0;
    }

    private int getVat(NotificationCostPaymentResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getAnalogCostDetail() != null ? costResponse.getTotalCost().getDetails().getAnalogCostDetail().getVat() : 0;
    }
}
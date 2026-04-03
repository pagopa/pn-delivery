package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.BaseCostComponent;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.BaseCostName;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.NotificationCostRecipientResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;

public class NotificationProcessCostResponseMapper {
    public NotificationProcessCostResponseInt fromExternal(NotificationProcessCostResponse ext) {
        if (ext == null) return null;
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

    public NotificationProcessCostResponseInt mapFromTimelineAndCostResponse(DeliveryInformationResponse timelineResponse, NotificationCostRecipientResponse costResponse) {
        if (timelineResponse == null || costResponse == null) return null;
        NotificationProcessCostResponseInt internal = new NotificationProcessCostResponseInt();
        internal.setPartialCost(getPartialCost(costResponse));
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

    private int getPartialCost(NotificationCostRecipientResponse costResponse) {
        int sendFee = getBaseCostComponent(BaseCostName.SEND_FEE, costResponse);
        int analogCost = getAnalogCost(costResponse);
        return sendFee + analogCost;
    }

    private int getBaseCostComponent(BaseCostName costName, NotificationCostRecipientResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getBaseCostDetail().getBaseCostComponents().stream()
                .filter(baseCostComponent -> baseCostComponent.getCostName().equals(costName))
                .mapToInt(BaseCostComponent::getCost)
                .findFirst().orElse(0);
    }

    private int getAnalogCost(NotificationCostRecipientResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getAnalogCostDetail() != null ? costResponse.getTotalCost().getDetails().getAnalogCostDetail().getCost() : 0;
    }

    private int getVat(NotificationCostRecipientResponse costResponse) {
        return costResponse.getTotalCost().getDetails().getAnalogCostDetail() != null ? costResponse.getTotalCost().getDetails().getAnalogCostDetail().getVat() : 0;
    }
}
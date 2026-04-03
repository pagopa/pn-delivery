package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.model.NotificationProcessCostResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.notificationcostservice.v1.model.*;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.DeliveryInformationResponse;
import it.pagopa.pn.delivery.generated.openapi.msclient.timelineservice.v1.model.RefinementOrViewedDateDetail;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationProcessCostResponseMapperTest {

    private final NotificationProcessCostResponseMapper mapper = new NotificationProcessCostResponseMapper();

    @Test
    void fromExternal_shouldMapAllFields() {
        NotificationProcessCostResponse ext = new NotificationProcessCostResponse();
        ext.setPartialCost(100);
        ext.setTotalCost(200);
        ext.setAnalogCost(50);
        ext.setPaFee(10);
        ext.setSendFee(20);
        ext.setVat(22);
        OffsetDateTime now = OffsetDateTime.now();
        ext.setRefinementDate(now);
        ext.setNotificationViewDate(now.plusDays(1));

        NotificationProcessCostResponseInt result = mapper.fromExternal(ext);
        assertNotNull(result);
        assertEquals(100, result.getPartialCost());
        assertEquals(200, result.getTotalCost());
        assertEquals(50, result.getAnalogCost());
        assertEquals(10, result.getPaFee());
        assertEquals(20, result.getSendFee());
        assertEquals(22, result.getVat());
        assertEquals(now, result.getRefinementDate());
        assertEquals(now.plusDays(1), result.getNotificationViewDate());
    }

    @Test
    void fromExternalTimelineAndCost_shouldMapAllFields() {
        // Setup timeline response
        OffsetDateTime refinementDate = OffsetDateTime.now();
        OffsetDateTime viewedDate = refinementDate.plusDays(1);
        RefinementOrViewedDateDetail detail = new RefinementOrViewedDateDetail()
                .refinementDate(refinementDate)
                .viewedDate(viewedDate);
        DeliveryInformationResponse timeline = new DeliveryInformationResponse();
        timeline.setRefinementOrViewedDateDetail(detail);

        // Setup cost response
        BaseCostComponent paFee = new BaseCostComponent().cost(10).costName(BaseCostName.PA_FEE);
        BaseCostComponent sendFee = new BaseCostComponent().cost(20).costName(BaseCostName.SEND_FEE);
        BaseCostDetail baseCostDetail = new BaseCostDetail().baseCostComponents(List.of(paFee, sendFee));
        AnalogCostDetail analogCostDetail = new AnalogCostDetail().cost(50).vat(22);
        TotalCostDetails details = new TotalCostDetails().baseCostDetail(baseCostDetail).analogCostDetail(analogCostDetail);
        TotalCost totalCost = new TotalCost().costWithVat(200).details(details);
        NotificationCostRecipientResponse costResponse = new NotificationCostRecipientResponse().totalCost(totalCost);

        NotificationProcessCostResponseInt result = mapper.mapFromTimelineAndCostResponse(timeline, costResponse);
        assertNotNull(result);
        assertEquals(20 + 50, result.getPartialCost());
        assertEquals(200, result.getTotalCost());
        assertEquals(50, result.getAnalogCost());
        assertEquals(10, result.getPaFee());
        assertEquals(20, result.getSendFee());
        assertEquals(22, result.getVat());
        assertEquals(refinementDate, result.getRefinementDate());
        assertEquals(viewedDate, result.getNotificationViewDate());
    }
}

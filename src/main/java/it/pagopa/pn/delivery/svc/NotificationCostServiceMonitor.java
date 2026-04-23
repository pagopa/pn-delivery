package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.models.NotificationCostRequest;
import it.pagopa.pn.delivery.models.NotificationProcessCostResponseInt;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import it.pagopa.pn.delivery.utils.MetricUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Service
@CustomLog
@RequiredArgsConstructor
public class NotificationCostServiceMonitor {
    private final FeatureFlagUtils featureFlagUtils;
    private final NotificationCostServiceImpl notificationCostService;

    private static final Map<String, Function<NotificationProcessCostResponseInt, Integer>> COST_FIELDS =
            Map.of(
                    "partialCost", NotificationProcessCostResponseInt::getPartialCost,
                    "totalCost",   NotificationProcessCostResponseInt::getTotalCost,
                    "analogCost",  NotificationProcessCostResponseInt::getAnalogCost,
                    "paFee",       NotificationProcessCostResponseInt::getPaFee,
                    "sendFee",     NotificationProcessCostResponseInt::getSendFee,
                    "vat",         NotificationProcessCostResponseInt::getVat
            );

    public void monitorNewNotificationPriceService(
            InternalNotification notification,
            NotificationCostRequest costRequest,
            NotificationProcessCostResponseInt legacyResponse
    ) {
        String iun = notification.getIun();
        String paTaxId = costRequest.paTaxId();
        String noticeCode = costRequest.noticeCode();


        if (!featureFlagUtils.isMonitoringOfNewCostServiceEnabled(notification.getSentAt().toInstant())) {
            log.debug("Monitoring of new notification cost service is disabled for iun={} paTaxId={} noticeCode={}", iun, paTaxId, noticeCode);
            return;
        }

        try {
            NotificationProcessCostResponseInt newServiceResponse = notificationCostService.getNotificationCostForMonitoring(costRequest);
            logComparisonResult(notification, paTaxId, noticeCode, legacyResponse, newServiceResponse);
        } catch (Exception e) {
            log.error("Error monitoring new notification cost service for iun={} paTaxId={} noticeCode={}", iun, paTaxId, noticeCode, e);
        }
    }

    private void logComparisonResult(
            InternalNotification notification,
            String paTaxId,
            String noticeCode,
            NotificationProcessCostResponseInt legacy,
            NotificationProcessCostResponseInt newService) {

        if (legacy == null || newService == null) {
            log.error("Cannot compare notification costs: one or both responses are null for iun={} paTaxId={} noticeCode={}",
                    notification.getIun(), paTaxId, noticeCode);
            return;
        }

        List<String> differences = new ArrayList<>();
        StringBuilder detailBuilder = new StringBuilder("[Monitor] - Notification cost services response comparison completed. Detail: ");

        COST_FIELDS.forEach((fieldName, extractor) -> {
            Object legacyVal = extractor.apply(legacy);
            Object newVal = extractor.apply(newService);

            if (!Objects.equals(legacyVal, newVal)) {
                differences.add(fieldName);
            }
            detailBuilder.append(String.format("%s[L=%s, N=%s] ", fieldName, legacyVal, newVal));
        });

        String result = differences.isEmpty() ? "OK" : "KO";
        detailBuilder.append("Comparison result: ").append(result);
        GeneralMetric metric = buildComparisonMetric(result);

        log.logMetric(List.of(metric), detailBuilder.toString());
    }

    private GeneralMetric buildComparisonMetric(String result) {
        return MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.NOTIFICATION_PRICE_MONITOR_RESULT,
                1,
                List.of(new Dimension("Result", result))
        );
    }
}

package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class MetricUtils {
    private final static String METRIC_NAMESPACE = "pn-delivery";

    @Getter
    public enum MetricName {
        NOTIFICATION_PRICE_MONITOR_RESULT("notification_price_monitor_result");

        private final String value;

        MetricName(String value) {
            this.value = value;
        }
    }


    private MetricUtils() {
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, List<Dimension> dimensions) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setDimensions(dimensions);
        generalMetric.setTimestamp(Instant.now().toEpochMilli());
        return generalMetric;
    }

}

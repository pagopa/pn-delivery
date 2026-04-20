package it.pagopa.pn.delivery.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricUtilsTest {
    @Test
    void generateGeneralMetricSuccess() {
        // Given
        MetricUtils.MetricName metricName = MetricUtils.MetricName.NOTIFICATION_PRICE_MONITOR_RESULT;
        int metricValue = 100;
        List<Dimension> dimensions = List.of(
                new Dimension("env", "test")
        );

        // When
        GeneralMetric result = MetricUtils.generateGeneralMetric(metricName, metricValue, dimensions);

        // Then
        assertNotNull(result, "The generated metric should not be null");
        assertEquals("pn-delivery", result.getNamespace());
        assertEquals(dimensions, result.getDimensions());

        // Verifico la lista delle metriche
        assertNotNull(result.getMetrics());
        assertEquals(1, result.getMetrics().size());

        Metric innerMetric = result.getMetrics().get(0);
        assertEquals(metricName.getValue(), innerMetric.getName());
        assertEquals(metricValue, innerMetric.getValue());
    }

    @Test
    void metricNameEnumValues() {
        assertEquals("notification_price_monitor_result",
                MetricUtils.MetricName.NOTIFICATION_PRICE_MONITOR_RESULT.getValue());
    }
}
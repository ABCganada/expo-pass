package com.coderhan.lastmission.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.coderhan.lastmission.monitoring.domain.MetricSample;
import com.coderhan.lastmission.monitoring.domain.MetricSnapshot;
import com.coderhan.lastmission.monitoring.domain.MetricPoint;
import com.coderhan.lastmission.monitoring.domain.MetricTrend;
import com.coderhan.lastmission.monitoring.domain.MetricTrendDashboard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricQueryServiceTest {

    @Test
    void returnsMetricsProvidedByMonitoringCorePort() {
        MetricSnapshot expected = new MetricSnapshot(
                "{service_name=\"last-mission-backend\",service_instance_id=\"aaa-local\"}",
                List.of(new MetricSample(Map.of("job", "lastmission"), 1_785_852_578.996, "1")));
        MetricProvider provider = () -> expected;
        MetricQueryService service = new MetricQueryService(provider);

        MetricSnapshot result = service.findCurrentMetrics();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void returnsDailyTrendsProvidedByMonitoringCorePort() {
        MetricTrendDashboard expected = new MetricTrendDashboard(
                1_785_802_740,
                1_785_889_140,
                60,
                List.of(new MetricTrend(
                        "cpu-usage",
                        "프로세스 CPU 사용률",
                        "%",
                        "100 * avg(process_cpu_usage)",
                        List.of(new MetricPoint(1_785_889_140, "1.25")))));
        MetricProvider provider = new MetricProvider() {
            @Override
            public MetricSnapshot findCurrentMetrics() {
                return new MetricSnapshot("up", List.of());
            }

            @Override
            public MetricTrendDashboard findDailyTrends() {
                return expected;
            }
        };
        MetricQueryService service = new MetricQueryService(provider);

        MetricTrendDashboard result = service.findDailyTrends();

        assertThat(result).isEqualTo(expected);
    }
}

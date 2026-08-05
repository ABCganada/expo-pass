package com.coderhan.lastmission.monitoring.infrastructure;

import com.coderhan.lastmission.monitoring.application.MetricProvider;
import com.coderhan.lastmission.monitoring.domain.MetricSample;
import com.coderhan.lastmission.monitoring.domain.MetricSnapshot;
import com.coderhan.lastmission.monitoring.domain.MetricPoint;
import com.coderhan.lastmission.monitoring.domain.MetricTrend;
import com.coderhan.lastmission.monitoring.domain.MetricTrendDashboard;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class MonitoringCoreMetricProvider implements MetricProvider {
    private final RestClient restClient;
    private final String serviceName;
    private final String serviceInstanceId;

    MonitoringCoreMetricProvider(
            @Value("${MONITORING_CORE_BASE_URL}") String baseUrl,
            @Value("${SPRING_APPLICATION_NAME}") String serviceName,
            @Value("${OTEL_SERVICE_INSTANCE_ID}") String serviceInstanceId) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .build();
        this.serviceName = serviceName;
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public MetricSnapshot findCurrentMetrics() {
        CoreMetricResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/metrics/current")
                        .queryParam("serviceName", serviceName)
                        .queryParam("serviceInstanceId", serviceInstanceId)
                        .build())
                .retrieve()
                .body(CoreMetricResponse.class);

        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 응답을 반환했습니다.");
        }

        List<MetricSample> samples = response.samples() == null
                ? List.of()
                : response.samples().stream().map(CoreMetricSample::toDomain).toList();
        return new MetricSnapshot(response.query(), samples);
    }

    @Override
    public MetricTrendDashboard findDailyTrends() {
        CoreMetricTrendDashboardResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/metrics/trends/daily")
                        .queryParam("serviceName", serviceName)
                        .queryParam("serviceInstanceId", serviceInstanceId)
                        .build())
                .retrieve()
                .body(CoreMetricTrendDashboardResponse.class);

        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 응답을 반환했습니다.");
        }
        List<MetricTrend> metrics = response.metrics() == null
                ? List.of()
                : response.metrics().stream().map(CoreMetricTrend::toDomain).toList();
        return new MetricTrendDashboard(
                response.from(),
                response.to(),
                response.stepSeconds(),
                metrics);
    }

    private record CoreMetricResponse(String query, int sampleCount, List<CoreMetricSample> samples) {
    }

    private record CoreMetricSample(Map<String, String> labels, double timestamp, String value) {
        private MetricSample toDomain() {
            return new MetricSample(labels, timestamp, value);
        }
    }

    private record CoreMetricTrendDashboardResponse(
            double from,
            double to,
            int stepSeconds,
            List<CoreMetricTrend> metrics) {
    }

    private record CoreMetricTrend(
            String id,
            String title,
            String unit,
            String query,
            List<CoreMetricPoint> points) {

        private MetricTrend toDomain() {
            List<MetricPoint> metricPoints = points == null
                    ? List.of()
                    : points.stream().map(CoreMetricPoint::toDomain).toList();
            return new MetricTrend(id, title, unit, query, metricPoints);
        }
    }

    private record CoreMetricPoint(double timestamp, String value) {

        private MetricPoint toDomain() {
            return new MetricPoint(timestamp, value);
        }
    }
}

package com.coderhan.lastmission.monitoring.presentation;

import com.coderhan.lastmission.monitoring.application.MetricQueryService;
import com.coderhan.lastmission.monitoring.domain.MetricSample;
import com.coderhan.lastmission.monitoring.domain.MetricSnapshot;
import com.coderhan.lastmission.monitoring.domain.MetricPoint;
import com.coderhan.lastmission.monitoring.domain.MetricTrend;
import com.coderhan.lastmission.monitoring.domain.MetricTrendDashboard;
import com.coderhan.lastmission.shared.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/metrics")
@RequiredArgsConstructor
class MonitoringMetricController {
    private final MetricQueryService metricQueryService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<MetricResponse> current() {
        return ApiResponse.success(MetricResponse.from(metricQueryService.findCurrentMetrics()));
    }

    @GetMapping("/trends/daily")
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<MetricTrendDashboardResponse> dailyTrends() {
        return ApiResponse.success(
                MetricTrendDashboardResponse.from(metricQueryService.findDailyTrends()));
    }

    record MetricResponse(String query, int sampleCount, List<MetricSampleResponse> samples) {
        static MetricResponse from(MetricSnapshot snapshot) {
            List<MetricSampleResponse> samples = snapshot.samples().stream()
                    .map(MetricSampleResponse::from)
                    .toList();
            return new MetricResponse(snapshot.query(), samples.size(), samples);
        }
    }

    record MetricSampleResponse(Map<String, String> labels, double timestamp, String value) {
        static MetricSampleResponse from(MetricSample sample) {
            return new MetricSampleResponse(sample.labels(), sample.timestamp(), sample.value());
        }
    }

    record MetricTrendDashboardResponse(
            double from,
            double to,
            int stepSeconds,
            List<MetricTrendResponse> metrics) {

        static MetricTrendDashboardResponse from(MetricTrendDashboard dashboard) {
            return new MetricTrendDashboardResponse(
                    dashboard.from(),
                    dashboard.to(),
                    dashboard.stepSeconds(),
                    dashboard.metrics().stream().map(MetricTrendResponse::from).toList());
        }
    }

    record MetricTrendResponse(
            String id,
            String title,
            String unit,
            String query,
            List<MetricPointResponse> points) {

        static MetricTrendResponse from(MetricTrend trend) {
            return new MetricTrendResponse(
                    trend.id(),
                    trend.title(),
                    trend.unit(),
                    trend.query(),
                    trend.points().stream().map(MetricPointResponse::from).toList());
        }
    }

    record MetricPointResponse(double timestamp, String value) {

        static MetricPointResponse from(MetricPoint point) {
            return new MetricPointResponse(point.timestamp(), point.value());
        }
    }
}

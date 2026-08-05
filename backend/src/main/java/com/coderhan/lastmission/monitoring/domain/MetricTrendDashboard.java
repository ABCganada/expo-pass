package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record MetricTrendDashboard(
        double from,
        double to,
        int stepSeconds,
        List<MetricTrend> metrics) {

    public MetricTrendDashboard {
        metrics = List.copyOf(Objects.requireNonNull(metrics, "주요 메트릭은 필수입니다."));
    }
}

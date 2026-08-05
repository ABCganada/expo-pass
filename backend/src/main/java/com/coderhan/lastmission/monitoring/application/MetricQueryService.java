package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.MetricSnapshot;
import com.coderhan.lastmission.monitoring.domain.MetricTrendDashboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricQueryService {
    private final MetricProvider metricProvider;

    public MetricSnapshot findCurrentMetrics() {
        return metricProvider.findCurrentMetrics();
    }

    public MetricTrendDashboard findDailyTrends() {
        return metricProvider.findDailyTrends();
    }
}

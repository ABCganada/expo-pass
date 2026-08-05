package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.MetricSnapshot;
import com.coderhan.lastmission.monitoring.domain.MetricTrendDashboard;

public interface MetricProvider {
    MetricSnapshot findCurrentMetrics();

    default MetricTrendDashboard findDailyTrends() {
        throw new UnsupportedOperationException("24시간 메트릭 추이 조회를 지원하지 않습니다.");
    }
}

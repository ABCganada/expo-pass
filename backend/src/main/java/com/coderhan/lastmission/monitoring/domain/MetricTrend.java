package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record MetricTrend(
        String id,
        String title,
        String unit,
        String query,
        List<MetricPoint> points) {

    public MetricTrend {
        id = Objects.requireNonNull(id, "메트릭 ID는 필수입니다.");
        title = Objects.requireNonNull(title, "메트릭 제목은 필수입니다.");
        unit = Objects.requireNonNull(unit, "메트릭 단위는 필수입니다.");
        query = Objects.requireNonNull(query, "메트릭 쿼리는 필수입니다.");
        points = List.copyOf(Objects.requireNonNull(points, "메트릭 포인트는 필수입니다."));
    }
}

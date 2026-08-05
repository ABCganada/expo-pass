package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record MetricSnapshot(String query, List<MetricSample> samples) {

    public MetricSnapshot {
        query = Objects.requireNonNull(query, "메트릭 쿼리는 필수입니다.");
        samples = List.copyOf(Objects.requireNonNull(samples, "메트릭 샘플은 필수입니다."));
    }
}

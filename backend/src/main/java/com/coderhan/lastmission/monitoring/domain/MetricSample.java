package com.coderhan.lastmission.monitoring.domain;

import java.util.Map;
import java.util.Objects;

public record MetricSample(Map<String, String> labels, double timestamp, String value) {

    public MetricSample {
        labels = Map.copyOf(Objects.requireNonNull(labels, "메트릭 라벨은 필수입니다."));
        value = Objects.requireNonNull(value, "메트릭 값은 필수입니다.");
    }
}

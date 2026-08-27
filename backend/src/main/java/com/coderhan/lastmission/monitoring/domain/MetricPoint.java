package com.coderhan.lastmission.monitoring.domain;

import java.util.Objects;

public record MetricPoint(double timestamp, String value) {

    public MetricPoint {
        value = Objects.requireNonNull(value, "메트릭 값은 필수입니다.");
    }
}

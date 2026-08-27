package com.coderhan.lastmission.monitoring.domain;

import java.util.Map;
import java.util.Objects;

public record TraceSpanEvent(
        String name,
        double offsetMs,
        Map<String, String> attributes) {

    public TraceSpanEvent {
        name = Objects.requireNonNull(name, "이벤트 이름은 필수입니다.");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "이벤트 속성은 필수입니다."));
    }
}

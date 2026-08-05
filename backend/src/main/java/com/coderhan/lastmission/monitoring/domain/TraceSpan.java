package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TraceSpan(
        String spanId,
        String parentSpanId,
        String name,
        String serviceName,
        String kind,
        String status,
        double startOffsetMs,
        double durationMs,
        Map<String, String> attributes,
        List<TraceSpanEvent> events) {

    public TraceSpan {
        spanId = Objects.requireNonNull(spanId, "Span ID는 필수입니다.");
        name = Objects.requireNonNull(name, "Span 이름은 필수입니다.");
        serviceName = Objects.requireNonNull(serviceName, "서비스 이름은 필수입니다.");
        kind = Objects.requireNonNull(kind, "Span 종류는 필수입니다.");
        status = Objects.requireNonNull(status, "Span 상태는 필수입니다.");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "Span 속성은 필수입니다."));
        events = List.copyOf(Objects.requireNonNull(events, "Span 이벤트는 필수입니다."));
    }
}

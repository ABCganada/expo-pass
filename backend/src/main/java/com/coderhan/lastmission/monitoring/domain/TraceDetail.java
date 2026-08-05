package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record TraceDetail(
        String traceId,
        String rootServiceName,
        String rootTraceName,
        double startTime,
        double durationMs,
        boolean error,
        List<TraceSpan> spans) {

    public TraceDetail {
        traceId = Objects.requireNonNull(traceId, "Trace ID는 필수입니다.");
        rootServiceName = Objects.requireNonNull(rootServiceName, "루트 서비스 이름은 필수입니다.");
        rootTraceName = Objects.requireNonNull(rootTraceName, "루트 Trace 이름은 필수입니다.");
        spans = List.copyOf(Objects.requireNonNull(spans, "Span 목록은 필수입니다."));
    }
}

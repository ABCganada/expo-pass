package com.coderhan.lastmission.monitoring.domain;

import java.util.Objects;

public record TraceSummary(
        String traceId,
        String rootServiceName,
        String rootTraceName,
        double startTime,
        double durationMs,
        int spanCount,
        boolean error) {

    public TraceSummary {
        traceId = Objects.requireNonNull(traceId, "Trace ID는 필수입니다.");
        rootServiceName = Objects.requireNonNull(rootServiceName, "루트 서비스 이름은 필수입니다.");
        rootTraceName = Objects.requireNonNull(rootTraceName, "루트 Trace 이름은 필수입니다.");
    }
}

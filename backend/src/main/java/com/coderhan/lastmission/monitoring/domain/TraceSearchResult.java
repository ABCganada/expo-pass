package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record TraceSearchResult(
        int rangeMinutes,
        long searchedAtEpochSeconds,
        List<TraceSummary> traces) {

    public TraceSearchResult {
        traces = List.copyOf(Objects.requireNonNull(traces, "트레이스 목록은 필수입니다."));
    }
}

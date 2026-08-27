package com.coderhan.lastmission.monitoring.domain;

import java.util.List;
import java.util.Objects;

public record LogSearchResult(
        int rangeMinutes,
        long searchedAtEpochSeconds,
        List<LogEntry> logs,
        String nextCursor) {

    public LogSearchResult {
        logs = List.copyOf(Objects.requireNonNull(logs, "로그 목록은 필수입니다."));
        nextCursor = nextCursor == null ? "" : nextCursor;
    }
}

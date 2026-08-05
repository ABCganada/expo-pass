package com.coderhan.lastmission.monitoring.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.coderhan.lastmission.monitoring.domain.LogEntry;
import com.coderhan.lastmission.monitoring.domain.LogLevel;
import com.coderhan.lastmission.monitoring.domain.LogLevelSummary;
import com.coderhan.lastmission.monitoring.domain.LogSearchResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LogQueryServiceTest {
    @Test
    void delegatesOnlyToMonitoringLogPort() {
        AtomicReference<LogLevel> requestedLevel = new AtomicReference<>();
        LogSearchResult expected = new LogSearchResult(60, 1_785_902_400L, List.of(new LogEntry(
                "pod:1", 1_785_902_399, "ERROR", "failed", "Example", "main",
                "", "", "last-mission-backend", "pod", true, Map.of())), "");
        LogLevelSummary summary = new LogLevelSummary(1, 2, 3, 4, 5);
        LogProvider provider = new LogProvider() {
            @Override
            public LogSearchResult search(String podName, int rangeMinutes, LogLevel level,
                                          String keyword, String traceId, int limit, String cursor) {
                requestedLevel.set(level);
                return expected;
            }

            @Override
            public LogLevelSummary summarize(String podName, int rangeMinutes) {
                return summary;
            }
        };
        LogQueryService service = new LogQueryService(provider);

        assertThat(service.search("", 60, LogLevel.ERROR, "", "", 100, "")).isEqualTo(expected);
        assertThat(service.summarize("", 60)).isEqualTo(summary);
        assertThat(requestedLevel.get()).isEqualTo(LogLevel.ERROR);
    }
}

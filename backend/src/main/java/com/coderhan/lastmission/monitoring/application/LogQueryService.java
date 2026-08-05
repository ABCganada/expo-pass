package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.LogLevel;
import com.coderhan.lastmission.monitoring.domain.LogLevelSummary;
import com.coderhan.lastmission.monitoring.domain.LogSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogQueryService {
    private final LogProvider logProvider;

    public LogSearchResult search(
            String podName,
            int rangeMinutes,
            LogLevel level,
            String keyword,
            String traceId,
            int limit,
            String cursor) {
        return logProvider.search(podName, rangeMinutes, level, keyword, traceId, limit, cursor);
    }

    public LogLevelSummary summarize(String podName, int rangeMinutes) {
        return logProvider.summarize(podName, rangeMinutes);
    }
}

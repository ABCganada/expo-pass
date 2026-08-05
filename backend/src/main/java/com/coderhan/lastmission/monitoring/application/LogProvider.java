package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.LogLevel;
import com.coderhan.lastmission.monitoring.domain.LogLevelSummary;
import com.coderhan.lastmission.monitoring.domain.LogSearchResult;

public interface LogProvider {
    LogSearchResult search(
            String podName,
            int rangeMinutes,
            LogLevel level,
            String keyword,
            String traceId,
            int limit,
            String cursor);

    LogLevelSummary summarize(String podName, int rangeMinutes);
}

package com.coderhan.lastmission.monitoring.presentation;

import com.coderhan.lastmission.monitoring.application.LogQueryService;
import com.coderhan.lastmission.monitoring.domain.LogEntry;
import com.coderhan.lastmission.monitoring.domain.LogLevel;
import com.coderhan.lastmission.monitoring.domain.LogLevelSummary;
import com.coderhan.lastmission.monitoring.domain.LogSearchResult;
import com.coderhan.lastmission.shared.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/logs")
@RequiredArgsConstructor
class MonitoringLogController {
    private final LogQueryService logQueryService;

    @GetMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<LogSearchResponse> search(
            @RequestParam(defaultValue = "") String podName,
            @RequestParam(defaultValue = "60") int rangeMinutes,
            @RequestParam(defaultValue = "ALL") LogLevel level,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String traceId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "") String cursor) {
        return ApiResponse.success(LogSearchResponse.from(logQueryService.search(
                podName, rangeMinutes, level, keyword, traceId, limit, cursor)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<LogLevelSummaryResponse> summary(
            @RequestParam(defaultValue = "") String podName,
            @RequestParam(defaultValue = "60") int rangeMinutes) {
        return ApiResponse.success(LogLevelSummaryResponse.from(logQueryService.summarize(podName, rangeMinutes)));
    }

    record LogSearchResponse(int rangeMinutes, long searchedAtEpochSeconds,
                             List<LogEntryResponse> logs, String nextCursor) {
        static LogSearchResponse from(LogSearchResult result) {
            return new LogSearchResponse(result.rangeMinutes(), result.searchedAtEpochSeconds(),
                    result.logs().stream().map(LogEntryResponse::from).toList(), result.nextCursor());
        }
    }

    record LogEntryResponse(
            String id, double timestamp, String level, String message, String logger, String thread,
            String traceId, String spanId, String serviceName, String podName,
            boolean exception, Map<String, String> labels) {
        static LogEntryResponse from(LogEntry entry) {
            return new LogEntryResponse(entry.id(), entry.timestamp(), entry.level(), entry.message(),
                    entry.logger(), entry.thread(), entry.traceId(), entry.spanId(), entry.serviceName(),
                    entry.podName(), entry.exception(), entry.labels());
        }
    }

    record LogLevelSummaryResponse(long error, long warn, long info, long debug, long other) {
        static LogLevelSummaryResponse from(LogLevelSummary summary) {
            return new LogLevelSummaryResponse(
                    summary.error(), summary.warn(), summary.info(), summary.debug(), summary.other());
        }
    }
}

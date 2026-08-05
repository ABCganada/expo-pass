package com.coderhan.lastmission.monitoring.infrastructure;

import com.coderhan.lastmission.monitoring.application.LogProvider;
import com.coderhan.lastmission.monitoring.domain.LogEntry;
import com.coderhan.lastmission.monitoring.domain.LogLevel;
import com.coderhan.lastmission.monitoring.domain.LogLevelSummary;
import com.coderhan.lastmission.monitoring.domain.LogSearchResult;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class MonitoringCoreLogProvider implements LogProvider {
    private final RestClient restClient;
    private final String serviceName;
    private final String serviceNameAlias;

    MonitoringCoreLogProvider(
            @Value("${MONITORING_CORE_BASE_URL}") String baseUrl,
            @Value("${SPRING_APPLICATION_NAME}") String serviceName,
            @Value("${lastmission.monitoring.log-service-name-alias:}") String serviceNameAlias) {
        this.restClient = RestClient.builder().baseUrl(baseUrl.replaceAll("/+$", "")).build();
        this.serviceName = serviceName;
        this.serviceNameAlias = serviceNameAlias;
    }

    @Override
    public LogSearchResult search(
            String podName, int rangeMinutes, LogLevel level, String keyword,
            String traceId, int limit, String cursor) {
        CoreLogSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/logs")
                        .queryParam("serviceName", serviceName)
                        .queryParam("serviceNameAlias", serviceNameAlias)
                        .queryParam("podName", text(podName))
                        .queryParam("rangeMinutes", rangeMinutes)
                        .queryParam("level", level)
                        .queryParam("keyword", text(keyword))
                        .queryParam("traceId", text(traceId))
                        .queryParam("limit", limit)
                        .queryParam("cursor", text(cursor))
                        .build())
                .retrieve()
                .body(CoreLogSearchResponse.class);
        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 로그 검색 응답을 반환했습니다.");
        }
        List<LogEntry> logs = response.logs() == null
                ? List.of()
                : response.logs().stream().map(CoreLogEntry::toDomain).toList();
        return new LogSearchResult(
                response.rangeMinutes(), response.searchedAtEpochSeconds(), logs, response.nextCursor());
    }

    @Override
    public LogLevelSummary summarize(String podName, int rangeMinutes) {
        CoreLogLevelSummary response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/logs/summary")
                        .queryParam("serviceName", serviceName)
                        .queryParam("serviceNameAlias", serviceNameAlias)
                        .queryParam("podName", text(podName))
                        .queryParam("rangeMinutes", rangeMinutes)
                        .build())
                .retrieve()
                .body(CoreLogLevelSummary.class);
        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 로그 요약 응답을 반환했습니다.");
        }
        return new LogLevelSummary(response.error(), response.warn(), response.info(), response.debug(), response.other());
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private record CoreLogSearchResponse(
            int rangeMinutes, long searchedAtEpochSeconds, List<CoreLogEntry> logs, String nextCursor) {
    }

    private record CoreLogEntry(
            String id, double timestamp, String level, String message, String logger, String thread,
            String traceId, String spanId, String serviceName, String podName,
            boolean exception, Map<String, String> labels) {
        private LogEntry toDomain() {
            return new LogEntry(id, timestamp, level, message, logger, thread, traceId, spanId,
                    serviceName, podName, exception, labels);
        }
    }

    private record CoreLogLevelSummary(long error, long warn, long info, long debug, long other) {
    }
}

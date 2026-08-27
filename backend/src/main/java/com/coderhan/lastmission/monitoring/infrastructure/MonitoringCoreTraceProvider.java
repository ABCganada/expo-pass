package com.coderhan.lastmission.monitoring.infrastructure;

import com.coderhan.lastmission.monitoring.application.TraceProvider;
import com.coderhan.lastmission.monitoring.domain.TraceCategory;
import com.coderhan.lastmission.monitoring.domain.TraceDetail;
import com.coderhan.lastmission.monitoring.domain.TraceSearchResult;
import com.coderhan.lastmission.monitoring.domain.TraceSpan;
import com.coderhan.lastmission.monitoring.domain.TraceSpanEvent;
import com.coderhan.lastmission.monitoring.domain.TraceStatus;
import com.coderhan.lastmission.monitoring.domain.TraceSummary;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class MonitoringCoreTraceProvider implements TraceProvider {

    private final RestClient restClient;
    private final String serviceName;
    private final String serviceInstanceId;

    MonitoringCoreTraceProvider(
            @Value("${MONITORING_CORE_BASE_URL}") String baseUrl,
            @Value("${SPRING_APPLICATION_NAME}") String serviceName,
            @Value("${OTEL_SERVICE_INSTANCE_ID}") String serviceInstanceId) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .build();
        this.serviceName = serviceName;
        this.serviceInstanceId = serviceInstanceId;
    }

    @Override
    public TraceSearchResult search(
            TraceCategory category,
            int rangeMinutes,
            TraceStatus status,
            long minDurationMs,
            String operation,
            int limit) {
        CoreTraceSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/traces")
                        .queryParam("serviceName", serviceName)
                        .queryParam("serviceInstanceId", serviceInstanceId)
                        .queryParam("category", category)
                        .queryParam("rangeMinutes", rangeMinutes)
                        .queryParam("status", status)
                        .queryParam("minDurationMs", minDurationMs)
                        .queryParam("operation", operation == null ? "" : operation)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(CoreTraceSearchResponse.class);
        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 Trace 검색 응답을 반환했습니다.");
        }
        List<TraceSummary> traces = response.traces() == null
                ? List.of()
                : response.traces().stream().map(CoreTraceSummary::toDomain).toList();
        return new TraceSearchResult(response.rangeMinutes(), response.searchedAtEpochSeconds(), traces);
    }

    @Override
    public TraceDetail findById(String traceId) {
        CoreTraceDetail response = restClient.get()
                .uri("/api/traces/{traceId}", traceId)
                .retrieve()
                .body(CoreTraceDetail.class);
        if (response == null) {
            throw new IllegalStateException("Monitoring Core가 빈 Trace 상세 응답을 반환했습니다.");
        }
        List<TraceSpan> spans = response.spans() == null
                ? List.of()
                : response.spans().stream().map(CoreTraceSpan::toDomain).toList();
        return new TraceDetail(
                response.traceId(),
                response.rootServiceName(),
                response.rootTraceName(),
                response.startTime(),
                response.durationMs(),
                response.error(),
                spans);
    }

    private record CoreTraceSearchResponse(
            int rangeMinutes,
            long searchedAtEpochSeconds,
            List<CoreTraceSummary> traces) {
    }

    private record CoreTraceSummary(
            String traceId,
            String rootServiceName,
            String rootTraceName,
            double startTime,
            double durationMs,
            int spanCount,
            boolean error) {

        private TraceSummary toDomain() {
            return new TraceSummary(
                    traceId,
                    rootServiceName,
                    rootTraceName,
                    startTime,
                    durationMs,
                    spanCount,
                    error);
        }
    }

    private record CoreTraceDetail(
            String traceId,
            String rootServiceName,
            String rootTraceName,
            double startTime,
            double durationMs,
            boolean error,
            List<CoreTraceSpan> spans) {
    }

    private record CoreTraceSpan(
            String spanId,
            String parentSpanId,
            String name,
            String serviceName,
            String kind,
            String status,
            double startOffsetMs,
            double durationMs,
            Map<String, String> attributes,
            List<CoreTraceSpanEvent> events) {

        private TraceSpan toDomain() {
            List<TraceSpanEvent> spanEvents = events == null
                    ? List.of()
                    : events.stream().map(CoreTraceSpanEvent::toDomain).toList();
            return new TraceSpan(
                    spanId,
                    parentSpanId,
                    name,
                    serviceName,
                    kind,
                    status,
                    startOffsetMs,
                    durationMs,
                    attributes == null ? Map.of() : attributes,
                    spanEvents);
        }
    }

    private record CoreTraceSpanEvent(
            String name,
            double offsetMs,
            Map<String, String> attributes) {

        private TraceSpanEvent toDomain() {
            return new TraceSpanEvent(
                    name,
                    offsetMs,
                    attributes == null ? Map.of() : attributes);
        }
    }
}

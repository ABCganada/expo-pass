package com.coderhan.lastmission.monitoring.presentation;

import com.coderhan.lastmission.monitoring.application.TraceQueryService;
import com.coderhan.lastmission.monitoring.domain.TraceCategory;
import com.coderhan.lastmission.monitoring.domain.TraceDetail;
import com.coderhan.lastmission.monitoring.domain.TraceSearchResult;
import com.coderhan.lastmission.monitoring.domain.TraceSpan;
import com.coderhan.lastmission.monitoring.domain.TraceSpanEvent;
import com.coderhan.lastmission.monitoring.domain.TraceStatus;
import com.coderhan.lastmission.monitoring.domain.TraceSummary;
import com.coderhan.lastmission.shared.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/traces")
@RequiredArgsConstructor
class MonitoringTraceController {

    private final TraceQueryService traceQueryService;

    @GetMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<TraceSearchResponse> search(
            @RequestParam(defaultValue = "REQUEST") TraceCategory category,
            @RequestParam(defaultValue = "60") int rangeMinutes,
            @RequestParam(defaultValue = "ALL") TraceStatus status,
            @RequestParam(defaultValue = "0") long minDurationMs,
            @RequestParam(defaultValue = "") String operation,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(TraceSearchResponse.from(traceQueryService.search(
                category,
                rangeMinutes,
                status,
                minDurationMs,
                operation,
                limit)));
    }

    @GetMapping("/{traceId}")
    @PreAuthorize("hasRole('DEVELOPER')")
    ApiResponse<TraceDetailResponse> detail(@PathVariable String traceId) {
        return ApiResponse.success(TraceDetailResponse.from(traceQueryService.findById(traceId)));
    }

    record TraceSearchResponse(
            int rangeMinutes,
            long searchedAtEpochSeconds,
            List<TraceSummaryResponse> traces) {

        static TraceSearchResponse from(TraceSearchResult result) {
            return new TraceSearchResponse(
                    result.rangeMinutes(),
                    result.searchedAtEpochSeconds(),
                    result.traces().stream().map(TraceSummaryResponse::from).toList());
        }
    }

    record TraceSummaryResponse(
            String traceId,
            String rootServiceName,
            String rootTraceName,
            double startTime,
            double durationMs,
            int spanCount,
            boolean error) {

        static TraceSummaryResponse from(TraceSummary trace) {
            return new TraceSummaryResponse(
                    trace.traceId(),
                    trace.rootServiceName(),
                    trace.rootTraceName(),
                    trace.startTime(),
                    trace.durationMs(),
                    trace.spanCount(),
                    trace.error());
        }
    }

    record TraceDetailResponse(
            String traceId,
            String rootServiceName,
            String rootTraceName,
            double startTime,
            double durationMs,
            boolean error,
            List<TraceSpanResponse> spans) {

        static TraceDetailResponse from(TraceDetail trace) {
            return new TraceDetailResponse(
                    trace.traceId(),
                    trace.rootServiceName(),
                    trace.rootTraceName(),
                    trace.startTime(),
                    trace.durationMs(),
                    trace.error(),
                    trace.spans().stream().map(TraceSpanResponse::from).toList());
        }
    }

    record TraceSpanResponse(
            String spanId,
            String parentSpanId,
            String name,
            String serviceName,
            String kind,
            String status,
            double startOffsetMs,
            double durationMs,
            Map<String, String> attributes,
            List<TraceSpanEventResponse> events) {

        static TraceSpanResponse from(TraceSpan span) {
            return new TraceSpanResponse(
                    span.spanId(),
                    span.parentSpanId(),
                    span.name(),
                    span.serviceName(),
                    span.kind(),
                    span.status(),
                    span.startOffsetMs(),
                    span.durationMs(),
                    span.attributes(),
                    span.events().stream().map(TraceSpanEventResponse::from).toList());
        }
    }

    record TraceSpanEventResponse(
            String name,
            double offsetMs,
            Map<String, String> attributes) {

        static TraceSpanEventResponse from(TraceSpanEvent event) {
            return new TraceSpanEventResponse(
                    event.name(),
                    event.offsetMs(),
                    event.attributes());
        }
    }
}

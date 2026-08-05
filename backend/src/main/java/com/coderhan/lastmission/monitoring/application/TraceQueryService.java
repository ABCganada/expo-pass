package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.TraceCategory;
import com.coderhan.lastmission.monitoring.domain.TraceDetail;
import com.coderhan.lastmission.monitoring.domain.TraceSearchResult;
import com.coderhan.lastmission.monitoring.domain.TraceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TraceQueryService {

    private final TraceProvider traceProvider;

    public TraceSearchResult search(
            TraceCategory category,
            int rangeMinutes,
            TraceStatus status,
            long minDurationMs,
            String operation,
            int limit) {
        return traceProvider.search(
                category,
                rangeMinutes,
                status,
                minDurationMs,
                operation,
                limit);
    }

    public TraceDetail findById(String traceId) {
        return traceProvider.findById(traceId);
    }
}

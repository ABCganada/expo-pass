package com.coderhan.lastmission.monitoring.application;

import com.coderhan.lastmission.monitoring.domain.TraceCategory;
import com.coderhan.lastmission.monitoring.domain.TraceDetail;
import com.coderhan.lastmission.monitoring.domain.TraceSearchResult;
import com.coderhan.lastmission.monitoring.domain.TraceStatus;

public interface TraceProvider {

    TraceSearchResult search(
            TraceCategory category,
            int rangeMinutes,
            TraceStatus status,
            long minDurationMs,
            String operation,
            int limit);

    TraceDetail findById(String traceId);
}

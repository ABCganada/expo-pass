package com.coderhan.lastmission.monitoring.domain;

import java.util.Map;
import java.util.Objects;

public record LogEntry(
        String id,
        double timestamp,
        String level,
        String message,
        String logger,
        String thread,
        String traceId,
        String spanId,
        String serviceName,
        String podName,
        boolean exception,
        Map<String, String> labels) {

    public LogEntry {
        id = Objects.requireNonNull(id, "로그 ID는 필수입니다.");
        level = Objects.requireNonNull(level, "로그 레벨은 필수입니다.");
        message = Objects.requireNonNull(message, "로그 메시지는 필수입니다.");
        logger = logger == null ? "" : logger;
        thread = thread == null ? "" : thread;
        traceId = traceId == null ? "" : traceId;
        spanId = spanId == null ? "" : spanId;
        serviceName = serviceName == null ? "" : serviceName;
        podName = podName == null ? "" : podName;
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}

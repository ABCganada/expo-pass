package com.coderhan.lastmission.monitoring.domain;

public record LogLevelSummary(long error, long warn, long info, long debug, long other) {
}

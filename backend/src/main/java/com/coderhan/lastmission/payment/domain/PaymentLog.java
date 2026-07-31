package com.coderhan.lastmission.payment.domain;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record PaymentLog(
        long id,
        String paymentKey,
        String action,
        String requestPayload,
        String responsePayload,
        String webhookTransmissionId,
        OffsetDateTime createdAt
) {
}

package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "payment_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PaymentLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // DB 기본값 unique_rowid() — CockroachDB hot range 방지

    @Column(name = "payment_key")
    private String paymentKey;  // 토스 paymentKey 그대로 저장, FK 없음(논리적 참조)

    @Column(name = "action", nullable = false)
    private String action;  // 'REQUEST'/'APPROVE'/'CANCEL'/'REFUND'(우리 호출) 또는 토스 웹훅 eventType

    @Column(name = "webhook_transmission_id")
    private String webhookTransmissionId;  // 웹훅 재시도 중복 저장 방지용. 우리 쪽 API 호출 로그는 null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    static PaymentLogEntity create(String paymentKey, String action, String requestPayload,
                                    String responsePayload, String webhookTransmissionId, OffsetDateTime createdAt) {
        PaymentLogEntity entity = new PaymentLogEntity();
        entity.paymentKey = paymentKey;
        entity.action = action;
        entity.requestPayload = requestPayload;
        entity.responsePayload = responsePayload;
        entity.webhookTransmissionId = webhookTransmissionId;
        entity.createdAt = createdAt;
        return entity;
    }
}

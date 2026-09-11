package com.coderhan.lastmission.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.PaymentLogRepository.ApprovedPaymentLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaPaymentLogRepositoryTest {

    @Mock PaymentLogJpaRepository jpaRepository;
    @Mock PaymentLogJpaRepository.ApprovedPaymentLogRow row;

    @InjectMocks JpaPaymentLogRepository repository;

    @Test
    @DisplayName("orderId로 실제 승인 완료된 APPROVE 기록이 있으면 필요한 필드를 추려 반환한다")
    void returnsApprovedPaymentLogWhenApprovedLogExists() {
        when(row.getPaymentKey()).thenReturn("payment-key-1");
        when(row.getAmount()).thenReturn("10000");
        when(row.getMethod()).thenReturn("CARD");
        when(row.getApprovedAt()).thenReturn("2026-07-23T10:00:00Z");
        when(jpaRepository.findApprovedByOrderId("ORD-1")).thenReturn(Optional.of(row));

        Optional<ApprovedPaymentLog> result = repository.findApprovedByOrderId("ORD-1");

        assertThat(result).contains(new ApprovedPaymentLog(
                "payment-key-1", new BigDecimal("10000"), "CARD",
                OffsetDateTime.parse("2026-07-23T10:00:00Z")));
    }

    @Test
    @DisplayName("일치하는 승인 완료 기록이 없으면 빈 값을 반환한다")
    void returnsEmptyWhenNoApprovedLogExists() {
        when(jpaRepository.findApprovedByOrderId("ORD-1")).thenReturn(Optional.empty());

        assertThat(repository.findApprovedByOrderId("ORD-1")).isEmpty();
    }
}

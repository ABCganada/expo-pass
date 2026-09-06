package com.coderhan.lastmission.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.coderhan.lastmission.payment.application.PaymentLogRepository;
import com.coderhan.lastmission.payment.application.PaymentLogRepository.ApprovedPaymentLog;
import com.coderhan.lastmission.payment.application.PaymentService;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaPaymentOrderDirectoryTest {
    private static final OffsetDateTime PAID_AT = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock PaymentJpaRepository jpaRepository;
    @Mock PaymentLogRepository paymentLogRepository;
    @Mock PaymentService paymentService;

    @InjectMocks JpaPaymentOrderDirectory directory;

    @Test
    @DisplayName("넘겨받은 orderId 중 COMPLETED 결제가 있는 것만 반환한다")
    void returnsOnlyCompletedOrderIds() {
        PaymentEntity completedOne = new PaymentEntity("ORD-1", OrderType.RESERVATION, 7L, "key-1",
                BigDecimal.valueOf(10000), "CARD", "TOSS", "pg-1", "key-1", PAID_AT, PAID_AT);
        PaymentEntity completedTwo = new PaymentEntity("ORD-3", OrderType.ADVERTISEMENT, 7L, "key-3",
                BigDecimal.valueOf(30000), "CARD", "TOSS", "pg-3", "key-3", PAID_AT, PAID_AT);
        when(jpaRepository.findByOrderIdInAndStatus(Set.of("ORD-1", "ORD-2", "ORD-3"), PaymentStatus.COMPLETED))
                .thenReturn(List.of(completedOne, completedTwo));
        // ORD-2는 payment_logs에도 승인 기록이 없다 — 여전히 미결제로 남는다.
        when(paymentLogRepository.findApprovedByOrderId("ORD-2")).thenReturn(Optional.empty());

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1", "ORD-2", "ORD-3"), OrderType.RESERVATION);

        assertThat(result).containsExactlyInAnyOrder("ORD-1", "ORD-3");
    }

    @Test
    @DisplayName("입력이 비어있으면 조회 없이 빈 목록을 반환한다")
    void returnsEmptyListWithoutQueryingWhenInputIsEmpty() {
        List<String> result = directory.findCompletedOrderIds(Set.of(), OrderType.RESERVATION);

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).findByOrderIdInAndStatus(any(), any());
    }

    @Test
    @DisplayName("[#1 수정 확인] payments에 기록이 없어도 payment_logs에 승인 완료 기록이 있으면 사후 기록 후 결제완료로 반환한다")
    void reconcilesFromPaymentLogsWhenPaymentsRowIsMissing() {
        ApprovedPaymentLog approved = new ApprovedPaymentLog("key-1", BigDecimal.valueOf(10000), "CARD", PAID_AT);
        when(jpaRepository.findByOrderIdInAndStatus(Set.of("ORD-1"), PaymentStatus.COMPLETED))
                .thenReturn(List.of());
        when(paymentLogRepository.findApprovedByOrderId("ORD-1")).thenReturn(Optional.of(approved));

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1"), OrderType.RESERVATION);

        assertThat(result).containsExactly("ORD-1");
        verify(paymentService).reconcileApprovedPayment(eq("ORD-1"), eq(OrderType.RESERVATION), isNull(),
                eq("key-1"), eq(BigDecimal.valueOf(10000)), eq("CARD"), eq("ORD-1"), eq(PAID_AT));
    }

    @Test
    @DisplayName("payments에도 payment_logs에도 기록이 없으면 그대로 미결제로 남는다")
    void staysUnpaidWhenNeitherPaymentsNorPaymentLogsHaveRecord() {
        when(jpaRepository.findByOrderIdInAndStatus(Set.of("ORD-1"), PaymentStatus.COMPLETED))
                .thenReturn(List.of());
        when(paymentLogRepository.findApprovedByOrderId("ORD-1")).thenReturn(Optional.empty());

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1"), OrderType.RESERVATION);

        assertThat(result).isEmpty();
        verify(paymentService, never()).reconcileApprovedPayment(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("사후 기록이 실패한 주문은 결과에서 제외될 뿐, 나머지 주문 처리는 계속된다")
    void excludesOrderWhenReconcileFailsButContinuesBatch() {
        ApprovedPaymentLog approved = new ApprovedPaymentLog("key-1", BigDecimal.valueOf(10000), "CARD", PAID_AT);
        when(jpaRepository.findByOrderIdInAndStatus(Set.of("ORD-1", "ORD-2"), PaymentStatus.COMPLETED))
                .thenReturn(List.of());
        when(paymentLogRepository.findApprovedByOrderId("ORD-1")).thenReturn(Optional.of(approved));
        when(paymentLogRepository.findApprovedByOrderId("ORD-2")).thenReturn(Optional.of(approved));
        when(paymentService.reconcileApprovedPayment(eq("ORD-1"), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("주문 금액 불일치"));

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1", "ORD-2"), OrderType.RESERVATION);

        assertThat(result).containsExactly("ORD-2");
    }
}

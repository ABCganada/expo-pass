package com.coderhan.lastmission.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
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

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1", "ORD-2", "ORD-3"));

        assertThat(result).containsExactlyInAnyOrder("ORD-1", "ORD-3");
    }

    @Test
    @DisplayName("입력이 비어있으면 조회 없이 빈 목록을 반환한다")
    void returnsEmptyListWithoutQueryingWhenInputIsEmpty() {
        List<String> result = directory.findCompletedOrderIds(Set.of());

        assertThat(result).isEmpty();
        verify(jpaRepository, never()).findByOrderIdInAndStatus(any(), any());
    }

    @Test
    @DisplayName("[버그 재현 - #1] payments에 기록이 없으면 실제로는 PG 승인이 완료된 주문도 결제완료로 인식하지 못한다")
    void doesNotDetectPgApprovedOrderWhenPaymentsRowIsMissing() {
        // 결제 승인(PG)은 성공했지만 payments 테이블 저장이 실패해 row 자체가 없는 상황을 재현한다.
        when(jpaRepository.findByOrderIdInAndStatus(Set.of("ORD-1"), PaymentStatus.COMPLETED))
                .thenReturn(List.of());

        List<String> result = directory.findCompletedOrderIds(Set.of("ORD-1"));

        // TODO(#1): payment_logs의 APPROVE 감사 로그로 재확인하는 로직이 추가되면
        // 이 케이스도 결제완료로 판정되어야 한다. 지금은 그 수단이 없어 미결제로 오판되고,
        // 이 주문은 ReservationReconcileScheduler에 의해 잘못 취소된다.
        assertThat(result).isEmpty();
    }
}

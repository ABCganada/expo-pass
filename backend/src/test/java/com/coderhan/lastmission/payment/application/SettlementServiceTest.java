package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.PaymentEventQueryPort;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.payment.domain.Settlement;
import com.coderhan.lastmission.payment.domain.SettlementStatus;
import com.coderhan.lastmission.payment.domain.SettlementSummary;
import com.coderhan.lastmission.reservation.ReservationOrderDirectory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {
    private static final long EVENT_ID = 42L;
    private static final long SETTLEMENT_ID = 1L;
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 99L;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock SettlementRepository settlementRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock RefundRepository refundRepository;
    @Mock ReservationOrderDirectory reservationOrderDirectory;
    @Mock PaymentEventQueryPort paymentEventQueryPort;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks SettlementService service;

    /**
     * 정산 로직의 핵심: 부분 환불(REFUNDED)이 걸린 결제도 정산 대상에서 빠지지 않고
     * (결제액 - 환불액)만큼만 매출로 잡혀야 한다. payment2는 실제로 환불이 걸리면
     * status가 COMPLETED가 아니라 REFUNDED로 바뀌는 걸 그대로 재현한다.
     */
    @Test
    @DisplayName("환불이 걸려 REFUNDED로 바뀐 결제도 (결제액 - 환불액)만큼 총매출에 포함해 수수료와 정산액을 계산한다")
    void calculatesNetSalesAcrossOrdersAndAppliesCommission() {
        Payment payment1 = paymentWithAmount(1L, "ORD-1", BigDecimal.valueOf(10000), PaymentStatus.COMPLETED);
        Payment payment2 = paymentWithAmount(2L, "ORD-2", BigDecimal.valueOf(20000), PaymentStatus.REFUNDED);

        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(reservationOrderDirectory.findOrderIdsByEventId(EVENT_ID)).thenReturn(List.of("ORD-1", "ORD-2"));
        when(paymentRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(payment1));
        when(paymentRepository.findByOrderId("ORD-2")).thenReturn(Optional.of(payment2));
        when(refundRepository.findActiveByPaymentId(1L)).thenReturn(Optional.empty());
        when(refundRepository.findActiveByPaymentId(2L)).thenReturn(Optional.of(refundOf(BigDecimal.valueOf(6000))));

        service.create(EVENT_ID);

        // 총매출 = 10000 + (20000 - 6000) = 24000, 수수료 = 24000 * 5% = 1200, 정산액 = 22800
        verify(settlementRepository).save(EVENT_ID, BigDecimal.valueOf(24000), new BigDecimal("5.00"),
                BigDecimal.valueOf(1200), BigDecimal.valueOf(22800), OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("결제가 실패/취소된 주문은 총매출 계산에서 제외한다")
    void excludesFailedOrCancelledPaymentsFromTotalSales() {
        Payment failed = paymentWithAmount(1L, "ORD-1", BigDecimal.valueOf(10000), PaymentStatus.FAILED);
        Payment cancelled = paymentWithAmount(2L, "ORD-2", BigDecimal.valueOf(20000), PaymentStatus.CANCELLED);

        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(reservationOrderDirectory.findOrderIdsByEventId(EVENT_ID)).thenReturn(List.of("ORD-1", "ORD-2"));
        when(paymentRepository.findByOrderId("ORD-1")).thenReturn(Optional.of(failed));
        when(paymentRepository.findByOrderId("ORD-2")).thenReturn(Optional.of(cancelled));

        service.create(EVENT_ID);

        verify(settlementRepository).save(EVENT_ID, BigDecimal.ZERO, new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, OffsetDateTime.now(clock));
        verify(refundRepository, never()).findActiveByPaymentId(anyLong());
    }

    @Test
    @DisplayName("이미 생성된 정산이 있으면 아무 것도 하지 않는다(행사 종료 감지 중복 처리 방어)")
    void doesNothingWhenSettlementAlreadyExists() {
        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        service.create(EVENT_ID);

        verify(reservationOrderDirectory, never()).findOrderIdsByEventId(anyLong());
        verify(settlementRepository, never()).save(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("행사에 속한 주문이 없으면 매출 0원으로 정산을 생성한다")
    void createsZeroSettlementWhenNoOrdersFound() {
        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(reservationOrderDirectory.findOrderIdsByEventId(EVENT_ID)).thenReturn(List.of());

        service.create(EVENT_ID);

        verify(settlementRepository).save(EVENT_ID, BigDecimal.ZERO, new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 정산만 목록에 보인다")
    void listsOnlySettlementsForOwnedEvent() {
        Settlement owned = settlementFor(EVENT_ID);
        when(paymentEventQueryPort.findEventIdsManagedBy(USER_ID)).thenReturn(List.of(EVENT_ID));
        when(settlementRepository.findByEventIdIn(List.of(EVENT_ID))).thenReturn(List.of(owned));

        List<Settlement> result = service.list(USER_ID);

        assertThat(result).containsExactly(owned);
    }

    @Test
    @DisplayName("담당하는 행사가 없으면 정산 조회 자체를 하지 않고 빈 목록을 반환한다")
    void returnsEmptyListWhenCallerManagesNoEvents() {
        when(paymentEventQueryPort.findEventIdsManagedBy(USER_ID)).thenReturn(List.of());
        when(settlementRepository.findByEventIdIn(List.of())).thenReturn(List.of());

        List<Settlement> result = service.list(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 정산이면 상세 조회가 성공한다")
    void getsSettlementWhenCallerManagesItsEvent() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(paymentEventQueryPort.findEventManagerId(EVENT_ID)).thenReturn(Optional.of(USER_ID));

        Settlement result = service.get(USER_ID, SETTLEMENT_ID);

        assertThat(result).isSameAs(settlement);
    }

    @Test
    @DisplayName("본인이 담당하지 않는 행사의 정산이면 PAYMENT_SETTLEMENT_ACCESS_DENIED 예외를 던진다")
    void rejectsGetWhenCallerDoesNotManageItsEvent() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(paymentEventQueryPort.findEventManagerId(EVENT_ID)).thenReturn(Optional.of(OTHER_USER_ID));

        assertThatThrownBy(() -> service.get(USER_ID, SETTLEMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_SETTLEMENT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("담당 관리자를 알 수 없으면(null) NPE 없이 PAYMENT_SETTLEMENT_ACCESS_DENIED 예외를 던진다")
    void rejectsGetWithoutNpeWhenManagerIsUnknown() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(paymentEventQueryPort.findEventManagerId(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(USER_ID, SETTLEMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_SETTLEMENT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("정산 내역이 없으면 PAYMENT_SETTLEMENT_NOT_FOUND 예외를 던진다")
    void rejectsGetWhenSettlementNotFound() {
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(USER_ID, SETTLEMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_SETTLEMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("전체 매출 대시보드는 담당자 구분 없이 리포지토리의 전체 합계를 그대로 반환한다")
    void returnsRepositoryDashboardSummaryAsIs() {
        SettlementSummary summary = new SettlementSummary(
                BigDecimal.valueOf(100000), BigDecimal.valueOf(5000), 3L);
        when(settlementRepository.getDashboardSummary()).thenReturn(summary);

        SettlementSummary result = service.getDashboardSummary();

        assertThat(result).isSameAs(summary);
    }

    private static Settlement settlementFor(long eventId) {
        return Settlement.builder()
                .id(1L)
                .eventId(eventId)
                .totalSales(BigDecimal.valueOf(24000))
                .commissionRate(new BigDecimal("5.00"))
                .commissionAmount(BigDecimal.valueOf(1200))
                .netAmount(BigDecimal.valueOf(22800))
                .status(SettlementStatus.COMPLETED)
                .settledAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static Payment paymentWithAmount(long id, String orderId, BigDecimal amount, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .amount(amount)
                .status(status)
                .build();
    }

    private static Refund refundOf(BigDecimal amount) {
        return Refund.builder()
                .amount(amount)
                .status(RefundStatus.COMPLETED)
                .createdAt(NOW)
                .build();
    }
}

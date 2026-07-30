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
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.payment.domain.Settlement;
import com.coderhan.lastmission.payment.domain.SettlementStatus;
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
    @Mock EventOrderLookup eventOrderLookup;
    @Mock EventManagerLookup eventManagerLookup;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks SettlementService service;

    /** 정산 로직의 핵심: 여러 주문의 결제액 - 환불액을 순매출로 합산해 수수료 5%를 적용하는지. */
    @Test
    @DisplayName("행사에 속한 여러 주문의 결제액에서 환불액을 뺀 총매출로 수수료와 정산액을 계산해 저장한다")
    void calculatesNetSalesAcrossOrdersAndAppliesCommission() {
        Payment payment1 = paymentWithAmount(1L, "ORD-1", BigDecimal.valueOf(10000));
        Payment payment2 = paymentWithAmount(2L, "ORD-2", BigDecimal.valueOf(20000));

        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventOrderLookup.findOrderIdsByEventId(EVENT_ID)).thenReturn(List.of("ORD-1", "ORD-2"));
        when(paymentRepository.findByOrderIdAndStatus("ORD-1", PaymentStatus.COMPLETED)).thenReturn(Optional.of(payment1));
        when(paymentRepository.findByOrderIdAndStatus("ORD-2", PaymentStatus.COMPLETED)).thenReturn(Optional.of(payment2));
        when(refundRepository.findActiveByPaymentId(1L)).thenReturn(Optional.empty());
        when(refundRepository.findActiveByPaymentId(2L)).thenReturn(Optional.of(refundOf(BigDecimal.valueOf(6000))));

        service.create(EVENT_ID);

        // 총매출 = 10000 + (20000 - 6000) = 24000, 수수료 = 24000 * 5% = 1200, 정산액 = 22800
        verify(settlementRepository).save(EVENT_ID, BigDecimal.valueOf(24000), new BigDecimal("5.00"),
                BigDecimal.valueOf(1200), BigDecimal.valueOf(22800), OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("이미 생성된 정산이 있으면 아무 것도 하지 않는다(행사 종료 감지 중복 처리 방어)")
    void doesNothingWhenSettlementAlreadyExists() {
        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(true);

        service.create(EVENT_ID);

        verify(eventOrderLookup, never()).findOrderIdsByEventId(anyLong());
        verify(settlementRepository, never()).save(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("행사에 속한 주문이 없으면 매출 0원으로 정산을 생성한다")
    void createsZeroSettlementWhenNoOrdersFound() {
        when(settlementRepository.existsByEventId(EVENT_ID)).thenReturn(false);
        when(eventOrderLookup.findOrderIdsByEventId(EVENT_ID)).thenReturn(List.of());

        service.create(EVENT_ID);

        verify(settlementRepository).save(EVENT_ID, BigDecimal.ZERO, new BigDecimal("5.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 정산만 목록에 보인다")
    void listsOnlySettlementsForOwnedEvent() {
        Settlement owned = settlementFor(EVENT_ID);
        when(eventManagerLookup.findEventIdsManagedBy(USER_ID)).thenReturn(List.of(EVENT_ID));
        when(settlementRepository.findByEventIdIn(List.of(EVENT_ID))).thenReturn(List.of(owned));

        List<Settlement> result = service.list(USER_ID);

        assertThat(result).containsExactly(owned);
    }

    @Test
    @DisplayName("담당하는 행사가 없으면 정산 조회 자체를 하지 않고 빈 목록을 반환한다")
    void returnsEmptyListWhenCallerManagesNoEvents() {
        when(eventManagerLookup.findEventIdsManagedBy(USER_ID)).thenReturn(List.of());
        when(settlementRepository.findByEventIdIn(List.of())).thenReturn(List.of());

        List<Settlement> result = service.list(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 정산이면 상세 조회가 성공한다")
    void getsSettlementWhenCallerManagesItsEvent() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(eventManagerLookup.findEventManagerId(EVENT_ID)).thenReturn(USER_ID);

        Settlement result = service.get(USER_ID, SETTLEMENT_ID);

        assertThat(result).isSameAs(settlement);
    }

    @Test
    @DisplayName("본인이 담당하지 않는 행사의 정산이면 PAYMENT_SETTLEMENT_ACCESS_DENIED 예외를 던진다")
    void rejectsGetWhenCallerDoesNotManageItsEvent() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(eventManagerLookup.findEventManagerId(EVENT_ID)).thenReturn(OTHER_USER_ID);

        assertThatThrownBy(() -> service.get(USER_ID, SETTLEMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_SETTLEMENT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("담당 관리자를 알 수 없으면(null) NPE 없이 PAYMENT_SETTLEMENT_ACCESS_DENIED 예외를 던진다")
    void rejectsGetWithoutNpeWhenManagerIsUnknown() {
        Settlement settlement = settlementFor(EVENT_ID);
        when(settlementRepository.findById(SETTLEMENT_ID)).thenReturn(Optional.of(settlement));
        when(eventManagerLookup.findEventManagerId(EVENT_ID)).thenReturn(null);

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

    private static Payment paymentWithAmount(long id, String orderId, BigDecimal amount) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.COMPLETED)
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

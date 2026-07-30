package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.Settlement;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal COMMISSION_RATE_PERCENT = new BigDecimal("5.00");
    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EventOrderLookup eventOrderLookup;
    private final EventManagerLookup eventManagerLookup;
    private final Clock clock;

    /**
     * 행사 종료 시 정산 생성.
     * 이미 생성된 정산이 있으면 아무 것도 하지 않는다(멱등) - 행사 종료 감지가 중복 발행/재처리돼도 정산이 두 번 생기지 않게 하기 위함.
     *
     * 총매출 = 행사에 속한 주문의 완료 결제액 합 - 그 결제에 걸린 완료 환불액 합.
     */
    public void create(long eventId) {
        if (settlementRepository.existsByEventId(eventId)) {
            return;
        }

        BigDecimal totalSales = calculateTotalSales(eventId);
        BigDecimal commissionAmount = totalSales
                .multiply(COMMISSION_RATE_PERCENT)
                .divide(PERCENT_DIVISOR, 0, RoundingMode.HALF_UP);
        BigDecimal netAmount = totalSales.subtract(commissionAmount);

        settlementRepository.save(eventId, totalSales, COMMISSION_RATE_PERCENT, commissionAmount, netAmount,
                OffsetDateTime.now(clock));
    }

    /** 정산 목록 조회. 본인이 담당하는 행사의 정산만 보인다(역방향 조회로 SQL 단에서 필터링). */
    public List<Settlement> list(long userId) {
        List<Long> managedEventIds = eventManagerLookup.findEventIdsManagedBy(userId);
        return settlementRepository.findByEventIdIn(managedEventIds);
    }

    /** 정산 상세 조회. 본인이 담당하는 행사의 정산만 조회할 수 있다(정방향 조회). */
    public Settlement get(long userId, long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_SETTLEMENT_NOT_FOUND, "정산 내역을 찾을 수 없습니다."));

        Long managerId = eventManagerLookup.findEventManagerId(settlement.eventId());
        validateSettlementAccess(userId, managerId);

        return settlement;
    }

    private void validateSettlementAccess(long userId, Long managerId) {
        if (!Objects.equals(managerId, userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_SETTLEMENT_ACCESS_DENIED, "본인이 담당하는 행사의 정산만 조회할 수 있습니다.");
        }
    }

    private BigDecimal calculateTotalSales(long eventId) {
        return eventOrderLookup.findOrderIdsByEventId(eventId).stream()
                .flatMap(orderId -> paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED).stream())
                .map(this::netAmountAfterRefund)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal netAmountAfterRefund(Payment payment) {
        BigDecimal refunded = refundRepository.findActiveByPaymentId(payment.id())
                .map(Refund::amount)
                .orElse(BigDecimal.ZERO);

        return payment.amount().subtract(refunded);
    }
}

package com.coderhan.lastmission.reservation.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import com.coderhan.lastmission.shared.order.OrderType;
import com.coderhan.lastmission.shared.order.PaymentOrderDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트 유실 등으로 PENDING 상태로 남은 예약 주문을 주기적으로 정리한다.
 *
 * <p>결제가 완료됐는데 이벤트가 유실된 경우 → CONFIRMED로 보정.
 * 결제 시도 없이(또는 실패했는데 이벤트가 유실돼) 타임아웃된 경우 → CANCELLED 처리하고 재고 복원.
 * 타임아웃 기준: 접수 후 10분 경과, 5분마다 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ReservationReconcileScheduler {

    private static final long TIMEOUT_MINUTES = 10L;

    private final ReservationRepository repository;
    private final ReservationService reservationService;
    private final PaymentOrderDirectory paymentOrderDirectory;
    private final Clock clock;

    @Scheduled(cron = "0 */5 * * * *") // 5분마다
    void reconcile() {
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusMinutes(TIMEOUT_MINUTES);
        List<String> pendingOrderIds = repository.findPendingOrderIdsOlderThan(threshold);
        if (pendingOrderIds.isEmpty()) {
            return;
        }

        Set<String> paidOrderIds = Set.copyOf(
                paymentOrderDirectory.findCompletedOrderIds(pendingOrderIds, OrderType.RESERVATION));

        for (String orderId : pendingOrderIds) {
            if (paidOrderIds.contains(orderId)) {
                log.info("reconcile: 결제 완료됐으나 PENDING 예약 보정. orderId={}", orderId);
                reservationService.confirmOrder(orderId);
            } else {
                log.info("reconcile: 미결제 타임아웃 예약 취소. orderId={}", orderId);
                reservationService.cancelOrder(orderId);
            }
        }
    }
}

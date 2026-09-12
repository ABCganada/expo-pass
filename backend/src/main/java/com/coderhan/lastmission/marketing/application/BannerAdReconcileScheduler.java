package com.coderhan.lastmission.marketing.application;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import com.coderhan.lastmission.shared.order.PaymentOrderDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트 유실 등으로 PENDING 상태로 남은 광고를 주기적으로 정리한다.
 *
 * <p>결제가 완료됐는데 이벤트가 유실된 경우 → PAID로 보정(관리자 검토 대기).
 * 결제 시도 없이 타임아웃된 경우 → CANCELLED 처리.
 * 타임아웃 기준: 등록 후 1시간 경과.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BannerAdReconcileScheduler {

    private static final long TIMEOUT_MINUTES = 60L;

    private final BannerAdRepository adRepository;
    private final BannerAdService bannerAdService;
    private final PaymentOrderDirectory paymentOrderDirectory;
    private final Clock clock;

    @Scheduled(cron = "0 */30 * * * *") // 30분마다
    void reconcile() {
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusMinutes(TIMEOUT_MINUTES);
        List<String> pendingOrderIds = adRepository.findPendingOrderIdsOlderThan(threshold);
        if (pendingOrderIds.isEmpty()) {
            return;
        }

        Set<String> paidOrderIds = Set.copyOf(
                paymentOrderDirectory.findCompletedOrderIds(pendingOrderIds));

        for (String orderId : pendingOrderIds) {
            if (paidOrderIds.contains(orderId)) {
                log.info("reconcile: 결제 완료됐으나 PENDING 광고 보정. orderId={}", orderId);
                bannerAdService.markAsPaidByOrderId(orderId);
            } else {
                log.info("reconcile: 미결제 타임아웃 광고 취소. orderId={}", orderId);
                bannerAdService.cancelByOrderId(orderId);
            }
        }
    }
}

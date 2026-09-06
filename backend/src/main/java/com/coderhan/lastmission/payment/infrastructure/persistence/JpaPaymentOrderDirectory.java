package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import com.coderhan.lastmission.payment.application.PaymentLogRepository;
import com.coderhan.lastmission.payment.application.PaymentService;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.shared.order.OrderType;
import com.coderhan.lastmission.shared.order.PaymentOrderDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
class JpaPaymentOrderDirectory implements PaymentOrderDirectory {
    private final PaymentJpaRepository jpaRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentService paymentService;

    @Override
    public List<String> findCompletedOrderIds(Collection<String> orderIds, OrderType orderType) {
        if (orderIds.isEmpty()) {
            return List.of();
        }

        List<String> locallyCompleted = jpaRepository.findByOrderIdInAndStatus(orderIds, PaymentStatus.COMPLETED)
                .stream()
                .map(PaymentEntity::getOrderId)
                .toList();

        List<String> missingLocally = orderIds.stream()
                .filter(orderId -> !locallyCompleted.contains(orderId))
                .toList();

        if (missingLocally.isEmpty()) {
            return locallyCompleted;
        }

        List<String> reconciled = missingLocally.stream()
                .filter(orderId -> reconcileIfApprovedInPaymentLogs(orderId, orderType))
                .toList();

        return Stream.concat(locallyCompleted.stream(), reconciled.stream()).toList();
    }

    /** payment_logs 승인 기록으로 재확인해 사후 기록. 실패한 주문만 건너뛰고 배치는 계속. */
    private boolean reconcileIfApprovedInPaymentLogs(String orderId, OrderType orderType) {
        return paymentLogRepository.findApprovedByOrderId(orderId)
                .map(approved -> tryReconcile(orderId, orderType, approved))
                .orElse(false);
    }

    private boolean tryReconcile(String orderId, OrderType orderType, PaymentLogRepository.ApprovedPaymentLog approved) {
        try {
            paymentService.reconcileApprovedPayment(orderId, orderType, null,
                    approved.paymentKey(), approved.amount(), approved.method(), orderId, approved.approvedAt());
            log.info("payment_logs 기반 사후 기록 성공. orderId={}, paymentKey={}", orderId, approved.paymentKey());
            return true;
        } catch (RuntimeException e) {
            log.warn("payment_logs 기반 사후 기록 실패, 다음 실행에 재시도. orderId={}", orderId, e);
            return false;
        }
    }
}

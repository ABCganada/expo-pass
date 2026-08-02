package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import com.coderhan.lastmission.payment.PaymentOrderDirectory;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPaymentOrderDirectory implements PaymentOrderDirectory {
    private final PaymentJpaRepository jpaRepository;

    @Override
    public List<String> findCompletedOrderIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }

        return jpaRepository.findByOrderIdInAndStatus(orderIds, PaymentStatus.COMPLETED).stream()
                .map(PaymentEntity::getOrderId)
                .toList();
    }
}

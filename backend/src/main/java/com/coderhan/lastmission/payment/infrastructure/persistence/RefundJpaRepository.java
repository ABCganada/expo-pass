package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface RefundJpaRepository extends JpaRepository<RefundEntity, Long> {
    Optional<RefundEntity> findFirstByPaymentIdAndStatusIn(Long paymentId, List<RefundStatus> statuses);
}
